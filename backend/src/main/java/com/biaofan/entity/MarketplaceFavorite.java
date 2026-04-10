package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模板收藏实体类
 * 记录用户收藏的模板市场SOP模板
 */
@Data
@TableName("marketplace_favorite")
public class MarketplaceFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String templateId;
    private LocalDateTime createdAt;
}
