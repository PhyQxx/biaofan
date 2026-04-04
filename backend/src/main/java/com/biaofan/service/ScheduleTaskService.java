package com.biaofan.service;

import com.biaofan.entity.ScheduleTask;
import java.util.List;

public interface ScheduleTaskService {
    ScheduleTask getBySopId(Long sopId, Long userId);
    void create(Long sopId, Long userId, String cronExpression);
    void delete(Long sopId, Long userId);
    void updateEnabled(Long id, Long userId, Integer enabled);
    List<ScheduleTask> getDueTasks();
    List<ScheduleTask> getMyTasks(Long userId);
    void advanceNextFireTime(Long id, String cronExpression);
}
