package com.biaofan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 市场模板评论请求
 * 用于用户对市场中的SOP模板进行评分和评论
 */
@Data
public class MarketplaceReviewRequest {
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小值为1")
    @Max(value = 5, message = "评分最大值为5")
    private Integer rating;
    private String comment;
}
