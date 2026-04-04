package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

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
