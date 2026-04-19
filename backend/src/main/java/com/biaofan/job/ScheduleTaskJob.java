package com.biaofan.job;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.Notification;
import com.biaofan.entity.ScheduleTask;
import com.biaofan.entity.Sop;
import com.biaofan.mapper.NotificationMapper;
import com.biaofan.mapper.ScheduleTaskMapper;
import com.biaofan.mapper.SopMapper;
import com.biaofan.service.NotificationDispatcher;
import com.biaofan.service.ScheduleTaskService;
import com.biaofan.service.SopInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 定时任务Job
 * 处理SOP周期性实例生成、Cron任务触发、逾期检测等定时任务
 */

/**
 * 定时任务：SOP 周期实例生成
 * - 每分钟执行一次（@Scheduled(fixedRate = 60000)）
 * - 扫描所有 ScheduleTask（周期任务配置）
 * - 判断当前时间是否到达周期开始时间
 * - 如到达：创建 SopInstance（周期实例），并记录下次触发时间
 * - 支持 Cron 表达式灵活配置触发时间
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTaskJob {

    private final SopInstanceService instanceService;
    private final ScheduleTaskService scheduleTaskService;
    private final ScheduleTaskMapper scheduleTaskMapper;
    private final SopMapper sopMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationDispatcher notificationDispatcher;
    private final StringRedisTemplate redisTemplate;

    /**
     * 处理周期性SOP实例生成
     * 每分钟执行一次，检查需要自动生成的周期性实例
     */
    @Scheduled(fixedRate = 60000)
    public void processPeriodicInstances() {
        if (!redisTemplate.opsForValue().setIfAbsent("lock:processPeriodicInstances", "1", Duration.ofMinutes(2))) {
            log.info("[Job] processPeriodicInstances 已被其他实例锁定，跳过执行");
            return;
        }
        try {
            instanceService.generatePeriodicInstances();
        } catch (Exception e) {
            log.error("生成周期实例异常: {}", e.getMessage(), e);
        } finally {
            redisTemplate.delete("lock:processPeriodicInstances");
        }
    }

    /**
     * 处理Cron定时任务
     * 每分钟执行一次，消费ScheduleTask表中已到期的Cron任务
     * 发送系统内部通知和外部通知（飞书/邮箱等），并更新下次触发时间
     */
    @Scheduled(fixedRate = 60000)
    public void processCronTasks() {
        if (!redisTemplate.opsForValue().setIfAbsent("lock:processCronTasks", "1", Duration.ofMinutes(2))) {
            log.info("[Job] processCronTasks 已被其他实例锁定，跳过执行");
            return;
        }
        try {
            var dueTasks = scheduleTaskMapper.selectList(
                    new LambdaQueryWrapper<ScheduleTask>()
                            .eq(ScheduleTask::getEnabled, 1)
                            .le(ScheduleTask::getNextFireTime, LocalDateTime.now()));

            for (ScheduleTask task : dueTasks) {
                processCronTask(task);
            }
        } catch (Exception e) {
            log.error("Cron 任务处理异常: {}", e.getMessage(), e);
        } finally {
            redisTemplate.delete("lock:processCronTasks");
        }
    }

    /**
     * 处理单个Cron任务
     * 创建通知、发送外部通知、更新下次触发时间
     * @param task 待处理的定时任务
     */
    private void processCronTask(ScheduleTask task) {
        try {
            Sop sop = sopMapper.selectById(task.getSopId());
            if (sop == null) {
                log.warn("[CronTask] SOP 不存在 sopId={}", task.getSopId());
                return;
            }

            // 发送系统内部通知
            Notification notif = new Notification();
            notif.setUserId(task.getUserId());
            notif.setType("schedule_triggered");
            notif.setTitle("SOP 定时触发提醒");
            notif.setContent("您的 SOP《" + sop.getTitle() + "》已到执行时间，请及时处理。");
            notif.setSourceType("sop");
            notif.setSourceId(sop.getId());
            notif.setIsRead(0);
            notif.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(notif);

            // 发送外部通知（飞书/邮箱等）
            notificationDispatcher.dispatch(task.getUserId(), notif.getTitle(), notif.getContent());

            // 更新下次触发时间
            scheduleTaskService.advanceNextFireTime(task.getId(), task.getCronExpression());

            log.info("[CronTask] 触发通知已发送 taskId={} sopId={} userId={}",
                    task.getId(), task.getSopId(), task.getUserId());
        } catch (Exception e) {
            log.error("[CronTask] 处理失败 taskId={} error={}", task.getId(), e.getMessage(), e);
            try {
                // Advance next fire time to prevent infinite retry loop
                // Schedule it 5 minutes from now
                task.setNextFireTime(LocalDateTime.now().plusMinutes(5));
                scheduleTaskMapper.updateById(task);
            } catch (Exception ex) {
                log.error("更新任务状态失败 taskId={}", task.getId(), ex);
            }
        }
    }

    /**
     * 检测逾期SOP实例
     * 每5分钟执行一次，检查并标记超期的实例
     */
    @Scheduled(fixedRate = 300000)
    public void checkOverdue() {
        if (!redisTemplate.opsForValue().setIfAbsent("lock:checkOverdue", "1", Duration.ofMinutes(2))) {
            log.info("[Job] checkOverdue 已被其他实例锁定，跳过执行");
            return;
        }
        try {
            instanceService.markOverdueInstances();
        } catch (Exception e) {
            log.error("逾期检查异常: {}", e.getMessage(), e);
        } finally {
            redisTemplate.delete("lock:checkOverdue");
        }
    }

    /**
     * 清理90天前已读通知
     * 每日执行一次，防止通知表无限增长
     */
    @Scheduled(fixedRate = 86400000)
    public void cleanupOldNotifications() {
        if (!redisTemplate.opsForValue().setIfAbsent("lock:cleanupOldNotifications", "1", Duration.ofMinutes(30))) {
            return;
        }
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
            int deleted = notificationMapper.delete(
                new LambdaQueryWrapper<Notification>()
                    .lt(Notification::getCreatedAt, cutoff)
                    .eq(Notification::getIsRead, true) // Only delete read notifications
            );
            log.info("[Notification] 清理了 {} 条90天前已读通知", deleted);
        } finally {
            redisTemplate.delete("lock:cleanupOldNotifications");
        }
    }
}
