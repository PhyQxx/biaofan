package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("gamification_badge_definition")
public class GamificationBadgeDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String badgeKey;
    private String name;
    private String icon;
    private String rarity;
    private String description;
    @TableField("`condition`")
    private String condition;
    @TableField("reward_exp")
    private Integer rewardExp;
    @TableField("reward_score")
    private Integer rewardScore;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
