package com.biaofan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 市场模板使用请求
 * 用于用户从市场复制/使用他人分享的SOP模板
 */
@Data
public class MarketplaceTemplateUseRequest {
    @NotBlank(message = "SOP名称不能为空")
    private String sopName;
}
