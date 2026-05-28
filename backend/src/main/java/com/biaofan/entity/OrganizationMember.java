package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 组织成员实体类
 */
@Data
@TableName("organization_member")
public class OrganizationMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long orgId;
    
    private Long userId;
    
    /** 角色: owner / admin / member */
    private String role;
    
    @TableField("joined_at")
    private LocalDateTime joinedAt;
}
