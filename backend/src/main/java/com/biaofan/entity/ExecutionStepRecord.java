package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("execution_step_record")
public class ExecutionStepRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long executionId;
    private Integer stepIndex;
    private String stepTitle;
    private String checkData;  // JSON - check item values
    private String attachments; // JSON - uploaded files
    private LocalDateTime completedAt;
    private String notes;
}
