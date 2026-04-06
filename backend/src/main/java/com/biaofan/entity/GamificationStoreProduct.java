package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("gamification_store_product")
public class GamificationStoreProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String icon;
    private String description;
    private Integer price;
    private Integer stock;
    private Boolean active;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
