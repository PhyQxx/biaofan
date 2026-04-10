package com.biaofan.dto;

import lombok.Data;

/**
 * 市场模板审核请求
 * 用于管理员对用户提交的市场模板进行审核操作
 */
@Data
public class MarketplaceAuditRequest {
    private String status; // approved / rejected
    private String rejectReason;
}
