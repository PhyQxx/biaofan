package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 组织实体类
 */
@Data
@TableName("organization")
public class Organization {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父级组织 ID */
    private Long parentId;

    /** 组织路径，例如: ,1,5, (便于递归查询) */
    private String path;
    
    private String name;

    /** 组织类型: company (公司), department (部门) */
    private String type;
    
    private Long ownerId;
    
    private String inviteCode;
    
    private String description;
    
    private String logoUrl;
    
    private String webhookUrl;
    
    private String webhookType; // dingtalk / feishu
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
