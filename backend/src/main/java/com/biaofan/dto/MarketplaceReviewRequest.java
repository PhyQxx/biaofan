package com.biaofan.dto;

import lombok.Data;

/**
 * 市场模板评论请求
 * 用于用户对市场中的SOP模板进行评分和评论
 */
@Data
public class MarketplaceReviewRequest {
    private String userId;
    private Integer rating;
    private String comment;
}
