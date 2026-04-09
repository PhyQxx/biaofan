package com.biaofan.dto;

import lombok.Data;

@Data
public class MarketplaceTemplateSubmitRequest {
    private String sopId;
    private String title;
    private String description;
    private String category;
    private String subCategory;
    private String coverUrl;
}
