package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SOP 审核记录实体类
 */
@Data
@TableName("sop_approval_record")
public class SopApprovalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long sopId;
    
    private Long orgId;
    
    private Long submitterId;
    
    private Long reviewerId;
    
    /** 状态: pending / approved / rejected */
    private String status;
    
    private String comment;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;
}
