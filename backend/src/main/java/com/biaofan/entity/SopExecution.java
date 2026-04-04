package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sop_execution")
public class SopExecution {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sopId;
    private Integer sopVersion;
    private Long executorId;
    private String status; // pending / in_progress / completed / abnormal
    private Integer currentStep;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
