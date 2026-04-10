package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户商品实体类，记录用户已兑换的积分商城商品
 */
@Data
@TableName("gamification_user_product")
public class GamificationUserProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("product_id")
    private Long productId;
    private Boolean equipped;
    @TableField("redeemed_at")
    private LocalDateTime redeemedAt;
}
