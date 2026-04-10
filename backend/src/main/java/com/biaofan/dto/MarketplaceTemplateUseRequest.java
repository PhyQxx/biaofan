package com.biaofan.dto;

import lombok.Data;

/**
 * 市场模板使用请求
 * 用于用户从市场复制/使用他人分享的SOP模板
 */
@Data
public class MarketplaceTemplateUseRequest {
    private String userId;
    private String sopName;
}
