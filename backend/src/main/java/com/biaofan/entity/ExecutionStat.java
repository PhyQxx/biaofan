package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 执行统计实体类
 * 汇总SOP的执行统计数据，包括总执行次数、完成次数、平均耗时等
 */
@Data
@TableName("execution_stat")
public class ExecutionStat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sopId;
    private Integer totalCount;
    private Integer completedCount;
    private Integer avgDurationMinutes;
    private LocalDateTime lastExecutedAt;

    // 非持久化字段，用于前端展示SOP名称
    @TableField(exist = false)
    private String sopTitle;
}
