package com.biaofan.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.*;
import com.biaofan.mapper.*;
import com.biaofan.service.ScheduleTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTaskJob {

    private final ScheduleTaskService scheduleTaskService;
    private final SopMapper sopMapper;
    private final NotificationMapper notificationMapper;

    /**
     * 每分钟检查需要触发的定时任务
     * Spring Boot 会自动执行此方法（需在启动类或配置类加 @EnableScheduling）
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void processDueTasks() {
        List<ScheduleTask> dueTasks = scheduleTaskService.getDueTasks();
        if (dueTasks == null || dueTasks.isEmpty()) {
            return;
        }

        for (ScheduleTask task : dueTasks) {
            try {
                // 获取 SOP 信息
                Sop sop = sopMapper.selectById(task.getSopId());
                if (sop == null) {
                    log.warn("SOP {} 不存在，跳过定时触发", task.getSopId());
                    scheduleTaskService.advanceNextFireTime(task.getId(), task.getCronExpression());
                    continue;
                }

                // 1. 发送通知
                sendNotification(task, sop);

                // 2. 更新下次触发时间
                scheduleTaskService.advanceNextFireTime(task.getId(), task.getCronExpression());

                log.info("定时任务触发成功: SOP[id={}, title={}], userId={}",
                        sop.getId(), sop.getTitle(), task.getUserId());
            } catch (Exception e) {
                log.error("定时任务执行异常: taskId={}, error={}", task.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 发送定时触发通知
     * 类型: schedule_triggered
     */
    private void sendNotification(ScheduleTask task, Sop sop) {
        Notification notification = new Notification();
        notification.setUserId(task.getUserId());
        notification.setType("schedule_triggered");
        notification.setTitle("SOP 定时触发提醒");
        notification.setContent("您的 SOP《" + sop.getTitle() + "》已到执行时间，请及时处理。");
        notification.setSourceType("sop");
        notification.setSourceId(sop.getId());
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }
}
