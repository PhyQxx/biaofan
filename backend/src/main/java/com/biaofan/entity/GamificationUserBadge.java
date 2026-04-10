package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户徽章实体类，记录用户已解锁的徽章及解锁时间
 */
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
