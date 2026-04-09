package com.biaofan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.dto.MarketplaceAuditRequest;
import com.biaofan.dto.MarketplaceReviewRequest;
import com.biaofan.dto.MarketplaceTemplateSubmitRequest;
import com.biaofan.dto.MarketplaceTemplateUseRequest;
import com.biaofan.dto.Result;
import com.biaofan.entity.MarketplaceFavorite;
import com.biaofan.entity.MarketplaceReview;
import com.biaofan.entity.MarketplaceTemplate;
import com.biaofan.entity.User;
import com.biaofan.mapper.MarketplaceFavoriteMapper;
import com.biaofan.mapper.MarketplaceReviewMapper;
import com.biaofan.mapper.MarketplaceTemplateMapper;
import com.biaofan.mapper.UserMapper;
import com.biaofan.service.MarketplaceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;
    private final MarketplaceTemplateMapper templateMapper;
    private final MarketplaceReviewMapper reviewMapper;
    private final MarketplaceFavoriteMapper favoriteMapper;
    private final UserMapper userMapper;

    // ========== 2.4.1 GET /api/marketplace/templates — 模板列表 ==========
    @GetMapping("/templates")
    public Result<?> getTemplateList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<MarketplaceTemplate> result = marketplaceService.getTemplateList(category, keyword, sort, page, pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("page_size", result.getSize());
        data.put("templates", result.getRecords());
        return Result.ok(data);
    }

    // ========== 2.4.2 GET /api/marketplace/templates/:template_id — 模板详情 ==========
    @GetMapping("/templates/{templateId}")
    public Result<?> getTemplateDetail(
            @PathVariable String templateId,
            @RequestParam(required = false) String userId) {
        try {
            MarketplaceTemplate tpl = marketplaceService.getTemplateDetail(templateId, userId);
            Map<String, Object> data = new HashMap<>();
            data.put("template_id", tpl.getTemplateId());
            data.put("title", tpl.getTitle());
            data.put("description", tpl.getDescription());
            data.put("category", tpl.getCategory());
            data.put("sub_category", tpl.getSubCategory());
            data.put("cover_url", tpl.getCoverUrl());
            data.put("author_id", tpl.getAuthorId());
            data.put("author_name", tpl.getAuthorName());
            data.put("step_count", tpl.getStepCount());
            data.put("estimated_duration", tpl.getEstimatedDuration());
            data.put("avg_rating", tpl.getAvgRating());
            data.put("rating_count", tpl.getRatingCount());
            data.put("use_count", tpl.getUseCount());
            data.put("status", tpl.getStatus());
            data.put("is_favorited", userId != null && marketplaceService.isFavorited(templateId, userId));
            data.put("created_at", tpl.getCreatedAt());
            return Result.ok(data);
        } catch (RuntimeException e) {
            return Result.fail(404, e.getMessage());
        }
    }

    // ========== 2.4.3 POST /api/marketplace/templates/:template_id/use — 使用模板 ==========
    @PostMapping("/templates/{templateId}/use")
    public Result<?> useTemplate(
            @PathVariable String templateId,
            @RequestBody MarketplaceTemplateUseRequest req) {
        try {
            Long sopId = marketplaceService.useTemplate(templateId, req.getUserId(), req.getSopName());
            Map<String, Object> data = new HashMap<>();
            data.put("sop_id", sopId);
            data.put("sop_name", req.getSopName());
            return Result.ok(data);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    // ========== 2.4.4 POST /api/marketplace/templates/:template_id/favorite — 收藏模板 ==========
    @PostMapping("/templates/{templateId}/favorite")
    public Result<?> addFavorite(
            @PathVariable String templateId,
            @RequestParam String userId) {
        marketplaceService.addFavorite(templateId, userId);
        return Result.ok(Map.of("message", "已收藏"));
    }

    // ========== 2.4.4 DELETE /api/marketplace/templates/:template_id/favorite — 取消收藏 ==========
    @DeleteMapping("/templates/{templateId}/favorite")
    public Result<?> removeFavorite(
            @PathVariable String templateId,
            @RequestParam String userId) {
        marketplaceService.removeFavorite(templateId, userId);
        return Result.ok(Map.of("message", "已取消收藏"));
    }

    // ========== 2.4.5 GET /api/marketplace/templates/favorites — 用户收藏列表 ==========
    @GetMapping("/templates/favorites")
    public Result<?> getFavorites(
            @RequestParam String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        // Get favorite template IDs first
        Page<MarketplaceFavorite> favPage = new Page<>(page, pageSize);
        IPage<MarketplaceFavorite> favs = favoriteMapper.selectPage(favPage,
                new LambdaQueryWrapper<MarketplaceFavorite>()
                        .eq(MarketplaceFavorite::getUserId, userId)
                        .orderByDesc(MarketplaceFavorite::getCreatedAt));

        List<String> templateIds = favs.getRecords().stream()
                .map(MarketplaceFavorite::getTemplateId)
                .collect(Collectors.toList());

        List<MarketplaceTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<MarketplaceTemplate>()
                        .in(MarketplaceTemplate::getTemplateId, templateIds)
                        .eq(MarketplaceTemplate::getStatus, "approved"));

        Map<String, Object> data = new HashMap<>();
        data.put("total", favs.getTotal());
        data.put("page", page);
        data.put("page_size", pageSize);
        data.put("templates", templates);
        return Result.ok(data);
    }

    // ========== 2.4.6 POST /api/marketplace/templates — 提交模板审核 ==========
    @PostMapping("/templates")
    public Result<?> submitTemplate(
            @RequestParam String userId,
            @RequestBody MarketplaceTemplateSubmitRequest req) {
        try {
            User user = userMapper.selectById(Long.parseLong(userId));
            String userName = user != null ? user.getUsername() : "未知用户";
            marketplaceService.submitTemplate(
                    userId, userName,
                    req.getSopId(), req.getTitle(),
                    req.getDescription(), req.getCategory(),
                    req.getSubCategory(), req.getCoverUrl());
            return Result.ok(Map.of("message", "提交成功，等待审核"));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    // ========== 2.4.7 GET /api/marketplace/templates/:template_id/reviews — 获取评价列表 ==========
    @GetMapping("/templates/{templateId}/reviews")
    public Result<?> getReviews(
            @PathVariable String templateId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        IPage<MarketplaceReview> result = marketplaceService.getReviews(templateId, page, pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("page_size", result.getSize());
        data.put("reviews", result.getRecords());
        return Result.ok(data);
    }

    // ========== 2.4.8 POST /api/marketplace/templates/:template_id/reviews — 提交评价 ==========
    @PostMapping("/templates/{templateId}/reviews")
    public Result<?> addReview(
            @PathVariable String templateId,
            @RequestBody MarketplaceReviewRequest req) {
        try {
            String userName = "未知用户";
            User user = userMapper.selectById(Long.parseLong(req.getUserId()));
            if (user != null) userName = user.getUsername();
            marketplaceService.addReview(templateId, req.getUserId(), userName,
                    req.getRating(), req.getComment());
            return Result.ok(Map.of("message", "评价已提交"));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
