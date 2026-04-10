package com.biaofan.service;

import com.biaofan.entity.ScheduleTask;
import java.util.List;

/**
 * 定时任务服务接口
 * 提供SOP定时执行任务的CRUD管理，支持cron表达式配置
 */
public interface ScheduleTaskService {
    /**
     * 获取SOP关联的定时任务
     * @param sopId SOP ID
     * @param userId 用户ID
     * @return 定时任务实体（无则返回null）
     */
    ScheduleTask getBySopId(Long sopId, Long userId);

    /**
     * 为SOP创建定时任务
     * @param sopId SOP ID
     * @param userId 用户ID
     * @param cronExpression Cron表达式（如0 0 9 * * ?）
     */
    void create(Long sopId, Long userId, String cronExpression);

    /**
     * 删除SOP的定时任务
     * @param sopId SOP ID
     * @param userId 用户ID
     */
    void delete(Long sopId, Long userId);

    /**
     * 启用/禁用定时任务
     * @param id 任务ID
     * @param userId 用户ID
     * @param enabled 是否启用（1启用，0禁用）
     */
    void updateEnabled(Long id, Long userId, Integer enabled);

    /**
     * 获取所有已到期的定时任务
     * @return 待执行的任务列表
     */
    List<ScheduleTask> getDueTasks();

    /**
     * 获取当前用户的定时任务列表
     * @param userId 用户ID
     * @return 任务列表
     */
    List<ScheduleTask> getMyTasks(Long userId);

    /**
     * 更新任务的下次触发时间
     * 根据cron表达式计算新的触发时间并保存
     * @param id 任务ID
     * @param cronExpression Cron表达式
     */
    void advanceNextFireTime(Long id, String cronExpression);
}
