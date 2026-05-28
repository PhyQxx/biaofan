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
    
    private String name;
    
    private Long ownerId;
    
    private String inviteCode;
    
    private String description;
    
    private String logoUrl;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
