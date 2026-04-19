package com.biaofan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 市场模板提交请求
 * 用于用户向市场提交SOP模板的请求数据
 */
@Data
public class MarketplaceTemplateSubmitRequest {
    @NotNull(message = "SOP ID不能为空")
    private String sopId;
    @NotBlank(message = "标题不能为空")
    private String title;
    private String description;
    @NotBlank(message = "分类不能为空")
    private String category;
    private String subCategory;
    private String coverUrl;
}
