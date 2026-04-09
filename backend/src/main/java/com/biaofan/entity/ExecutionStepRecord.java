package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("check_items")
    private String checkData;
    private LocalDateTime completedAt;
    private String notes;
}
