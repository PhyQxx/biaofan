package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

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
