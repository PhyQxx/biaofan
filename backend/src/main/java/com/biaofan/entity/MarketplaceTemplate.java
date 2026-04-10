package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模板市场实体类
 * 存储用户在模板市场上分享的SOP模板信息，包括审核状态、评分等
 */
@Data
@TableName("marketplace_template")
public class MarketplaceTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateId;
    private String sopId;
    private String title;
    private String description;
    private String category;
    private String subCategory;
    private String coverUrl;
    private String authorId;
    private String authorName;
    private Integer stepCount;
    private String estimatedDuration;
    private BigDecimal avgRating;
    private Integer ratingCount;
    private Integer useCount;
    private String status; // pending/approved/rejected/offline
    private String rejectReason;
    private String auditedBy;
    private LocalDateTime auditedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
