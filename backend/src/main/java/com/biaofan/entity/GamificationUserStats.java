package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户游戏化统计实体类，记录用户的等级、经验值、积分、排名及连续活跃天数等信息
 */
@Data
@TableName("gamification_user_stats")
public class GamificationUserStats {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    private Integer level;
    private Integer exp;
    @TableField("total_score")
    private Integer totalScore;
    @TableField("available_score")
    private Integer availableScore;
    @TableField("`rank`")
    private String rank;
    @TableField("streak_days")
    private Integer streakDays;
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
