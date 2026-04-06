package com.biaofan.dto;

import lombok.Data;

@Data
public class MarketplaceAuditRequest {
    private String status; // approved / rejected
    private String rejectReason;
}
