package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("gamification_leaderboard_cache")
public class GamificationLeaderboardCache {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String period;
    private Long userId;
    private String username;
    private Integer score;
    @TableField("`rank`")
    private Integer rank;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
