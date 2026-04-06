package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("gamification_user_badge")
public class GamificationUserBadge {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("badge_id")
    private Long badgeId;
    @TableField("unlocked_at")
    private LocalDateTime unlockedAt;
}
