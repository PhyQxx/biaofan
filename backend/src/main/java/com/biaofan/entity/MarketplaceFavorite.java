package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("marketplace_favorite")
public class MarketplaceFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String templateId;
    private LocalDateTime createdAt;
}
