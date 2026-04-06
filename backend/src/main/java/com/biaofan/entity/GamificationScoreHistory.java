package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("gamification_score_history")
public class GamificationScoreHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private Integer amount;
    private String reason;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
