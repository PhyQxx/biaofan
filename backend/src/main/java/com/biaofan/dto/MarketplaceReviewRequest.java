package com.biaofan.dto;

import lombok.Data;

@Data
public class MarketplaceReviewRequest {
    private String userId;
    private Integer rating;
    private String comment;
}
