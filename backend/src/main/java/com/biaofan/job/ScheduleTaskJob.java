package com.biaofan.job;

import com.biaofan.service.SopInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTaskJob {

    private final SopInstanceService instanceService;

    @Scheduled(fixedRate = 60000)
    public void processPeriodicInstances() {
        try {
            instanceService.generatePeriodicInstances();
        } catch (Exception e) {
            log.error("生成周期实例异常: {}", e.getMessage(), e);
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
