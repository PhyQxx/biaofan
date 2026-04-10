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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

    @Scheduled(fixedRate = 60000)
    public void processPeriodicInstances() {
        try {
            instanceService.generatePeriodicInstances();
        } catch (Exception e) {
            log.error("生成周期实例异常: {}", e.getMessage(), e);
        }
    }

    /** 每分钟跑一次，消费 ScheduleTask 表中已到期的 cron 任务 */
    @Scheduled(fixedRate = 60000)
    public void processCronTasks() {
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
        }
    }

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
            log.error("[CronTask] 处理失败 taskId={} error={}", task.getId(), e.getMessage());
        }
    }

    @Scheduled(fixedRate = 300000)
    public void checkOverdue() {
        try {
            instanceService.markOverdueInstances();
        } catch (Exception e) {
            log.error("逾期检查异常: {}", e.getMessage(), e);
        }
    }
}
