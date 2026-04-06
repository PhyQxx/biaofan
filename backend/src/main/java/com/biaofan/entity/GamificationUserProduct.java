package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("gamification_user_product")
public class GamificationUserProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    @TableField("product_id")
    private Long productId;
    private Boolean equipped;
    @TableField("redeemed_at")
    private LocalDateTime redeemedAt;
}
