package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 积分历史记录实体类，记录用户积分的增减变动明细
 */
@Data
@TableName("gamification_score_history")
public class GamificationScoreHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    private String type;
    private Integer amount;
    private String reason;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
