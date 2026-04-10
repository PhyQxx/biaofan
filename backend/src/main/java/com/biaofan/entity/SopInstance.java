package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SOP实例实体类
 * 表示一个具体的SOP执行任务实例，包含执行周期和当前进度信息
 */
@Data
@TableName("sop_instance")
public class SopInstance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sopId;
    private Integer sopVersion;
    private Long executorId;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private String status;
    private Integer currentStep;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
