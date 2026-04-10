package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模板评价实体类
 * 存储用户对模板市场上SOP模板的评价和评分
 */
@Data
@TableName("marketplace_review")
public class MarketplaceReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateId;
    private String userId;
    private String userName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
