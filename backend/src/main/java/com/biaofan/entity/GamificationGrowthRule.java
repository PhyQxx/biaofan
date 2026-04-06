package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("gamification_growth_rules")
public class GamificationGrowthRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleType;
    private String ruleKey;
    private String ruleValue;
    private Integer version;
    private Boolean isActive;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
