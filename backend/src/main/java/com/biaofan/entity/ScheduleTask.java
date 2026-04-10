package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 定时任务实体类
 * 配置SOP的定时执行任务，使用cron表达式控制触发时间
 */
@Data
@TableName("schedule_task")
public class ScheduleTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sopId;
    private Long userId;
    private String cronExpression;   // cron表达式
    private LocalDateTime nextFireTime; // 下次触发时间
    private Integer enabled;         // 0=禁用 1=启用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
