package com.biaofan.dto;

import lombok.Data;

/**
 * 市场模板提交请求
 * 用于用户向市场提交SOP模板的请求数据
 */
@Data
public class MarketplaceTemplateSubmitRequest {
    private String sopId;
    private String title;
    private String description;
    private String category;
    private String subCategory;
    private String coverUrl;
}
