package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 徽章定义实体类，定义系统中所有可获得的徽章及其获取条件、奖励等信息
 */
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
