package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 执行步骤记录实体类
 * 记录SOP执行过程中每个步骤的完成情况，包括检查项数据和备注
 */
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
