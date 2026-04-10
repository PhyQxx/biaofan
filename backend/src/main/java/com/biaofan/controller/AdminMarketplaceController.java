package com.biaofan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.biaofan.dto.MarketplaceAuditRequest;
import com.biaofan.dto.Result;
import com.biaofan.entity.MarketplaceTemplate;
import com.biaofan.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/marketplace")
@RequiredArgsConstructor
public class AdminMarketplaceController {

    private final MarketplaceService marketplaceService;

    // ========== 2.4.9 GET /api/admin/marketplace/templates — 管理员获取审核列表 ==========
    @GetMapping("/templates")
    public Result<?> getAuditList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<MarketplaceTemplate> result = marketplaceService.getAuditList(status, page, pageSize);
        Map<String, Object> data = Map.of(
                "total", result.getTotal(),
                "page", (long) result.getCurrent(),
                "page_size", (long) result.getSize(),
                "templates", result.getRecords()
        );
        return Result.ok(data);
    }

    // ========== 2.4.10 PUT /api/admin/marketplace/templates/:template_id/audit — 管理员审核模板 ==========
    @PutMapping("/templates/{templateId}/audit")
    public Result<?> auditTemplate(
            @PathVariable String templateId,
            @AuthenticationPrincipal Long userId,
            @RequestBody MarketplaceAuditRequest req) {
        try {
            // C-05: auditorId 从 @AuthenticationPrincipal 获取，而非 @RequestParam
            marketplaceService.auditTemplate(templateId, String.valueOf(userId), req.getStatus(), req.getRejectReason());
            return Result.ok(Map.of("message", "审核完成"));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    // ========== 2.4.11 DELETE /api/admin/marketplace/templates/:template_id — 管理员下架模板 ==========
    @DeleteMapping("/templates/{templateId}")
    public Result<?> offlineTemplate(@PathVariable String templateId) {
        try {
            marketplaceService.offlineTemplate(templateId);
            return Result.ok(Map.of("message", "模板已下架"));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
