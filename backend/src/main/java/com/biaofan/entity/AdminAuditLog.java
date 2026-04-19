package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Admin Audit Log Entity
 * 
 * KNOWN LIMITATION: This entity exists but the full audit aspect has not yet been implemented.
 * Planned: AdminAuditAspect to intercept Admin*Controller methods and log all mutations.
 * 
 * Actions tracked: CREATE_BADGE, AUDIT_TEMPLATE, UPDATE_PRODUCT, UPDATE_USER, etc.
 */
@Data
@TableName("admin_audit_log")
public class AdminAuditLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** Admin user ID who performed the action */
    private Long adminId;
    
    /** Action identifier: CREATE_BADGE, AUDIT_TEMPLATE, UPDATE_PRODUCT, UPDATE_USER, etc. */
    private String action;
    
    /** Target entity type: Badge, Template, Product, User, etc. */
    private String targetType;
    
    /** Target entity ID */
    private Long targetId;
    
    /** JSON containing old/new values */
    private String details;
    
    /** Client IP address */
    private String ip;
    
    /** Action timestamp */
    private LocalDateTime createdAt;
}
