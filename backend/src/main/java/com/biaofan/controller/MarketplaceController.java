package com.biaofan.controller;


/**
 * 模板市场 Controller（用户端）
 * - GET  /api/marketplace/templates: 模板列表（支持分类、搜索、排序）
 * - GET  /api/marketplace/templates/{id}: 模板详情
 * - POST /api/marketplace/templates: 提交模板到市场（审核）
 * - POST /api/marketplace/templates/{id}/use: 使用模板创建执行单
 * - POST /api/marketplace/templates/{id}/favorite: 收藏/取消收藏
 * - GET  /api/marketplace/favorites: 我的收藏列表
 * - POST /api/marketplace/templates/{id}/review: 对模板评分评论
 */
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.dto.MarketplaceReviewRequest;
import com.biaofan.dto.MarketplaceTemplateSubmitRequest;
import com.biaofan.dto.MarketplaceTemplateUseRequest;
import com.biaofan.dto.Result;
import com.biaofan.entity.MarketplaceFavorite;
import com.biaofan.entity.MarketplaceReview;
import com.biaofan.entity.MarketplaceTemplate;
import com.biaofan.entity.User;
import com.biaofan.service.MarketplaceService;
import com.biaofan.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板市场控制器
 * 提供模板市场相关的API，包括模板浏览、收藏、使用、评价等功能
 */

/**
 * 模板市场 Controller（用户端）
 * - GET  /api/marketplace/templates: 模板列表（支持分类、搜索、排序）
 * - GET  /api/marketplace/templates/{id}: 模板详情
 * - POST /api/marketplace/templates: 提交模板到市场（审核）
 * - POST /api/marketplace/templates/{id}/use: 使用模板创建执行单
 * - POST /api/marketplace/templates/{id}/favorite: 收藏/取消收藏
 * - GET  /api/marketplace/favorites: 我的收藏列表
 * - POST /api/marketplace/templates/{id}/review: 对模板评分评论
 */
@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;
    // M-09: 移除直接注入的 Mapper，通过 Service 操作
    private final UserService userService;

    /**
     * 获取模板列表
     * @param category 分类筛选（可选）
     * @param keyword 关键词搜索（可选）
     * @param sort 排序方式：recent（最新）、popular（最热）、rating（评分）
     * @param page 页码（默认1）
     * @param pageSize 每页数量（默认20）
     * @return 模板分页列表
     */
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

    /**
     * 获取模板详情
     * @param templateId 模板ID
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 模板详细信息，包含是否已收藏状态
     */
    // ========== 2.4.2 GET /api/marketplace/templates/:template_id — 模板详情 ==========
    @GetMapping("/templates/{templateId}")
    public Result<?> getTemplateDetail(
            @PathVariable String templateId,
            @AuthenticationPrincipal Long userId) {
        try {
            // C-04: userId 从 @AuthenticationPrincipal 获取
            String userIdStr = userId != null ? String.valueOf(userId) : null;
            MarketplaceTemplate tpl = marketplaceService.getTemplateDetail(templateId, userIdStr);
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
            data.put("is_favorited", userIdStr != null && marketplaceService.isFavorited(templateId, userIdStr));
            data.put("created_at", tpl.getCreatedAt());
            return Result.ok(data);
        } catch (RuntimeException e) {
            return Result.fail(404, e.getMessage());
        }
    }

    /**
     * 使用模板
     * 将市场模板复制到用户自己的SOP列表中
     * @param templateId 模板ID
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @param req 使用请求，包含SOP名称
     * @return 新创建的SOP ID和名称
     */
    // ========== 2.4.3 POST /api/marketplace/templates/:template_id/use — 使用模板 ==========
    @PostMapping("/templates/{templateId}/use")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<?> useTemplate(
            @PathVariable String templateId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MarketplaceTemplateUseRequest req) {
        try {
            // C-04: userId 从 @AuthenticationPrincipal 获取，而非 request body
            Long sopId = marketplaceService.useTemplate(templateId, String.valueOf(userId), req.getSopName());
            Map<String, Object> data = new HashMap<>();
            data.put("sop_id", sopId);
            data.put("sop_name", req.getSopName());
            return Result.ok(data);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 收藏模板
     * @param templateId 模板ID
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 操作结果
     */
    // ========== 2.4.4 POST /api/marketplace/templates/:template_id/favorite — 收藏模板 ==========
    @PostMapping("/templates/{templateId}/favorite")
    public Result<?> addFavorite(
            @PathVariable String templateId,
            @AuthenticationPrincipal Long userId) {
        // C-04: userId 从 @AuthenticationPrincipal 获取
        marketplaceService.addFavorite(templateId, String.valueOf(userId));
        return Result.ok(Map.of("message", "已收藏"));
    }

    /**
     * 取消收藏模板
     * @param templateId 模板ID
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 操作结果
     */
    // ========== 2.4.4 DELETE /api/marketplace/templates/:template_id/favorite — 取消收藏 ==========
    @DeleteMapping("/templates/{templateId}/favorite")
    public Result<?> removeFavorite(
            @PathVariable String templateId,
            @AuthenticationPrincipal Long userId) {
        // C-04: userId 从 @AuthenticationPrincipal 获取
        marketplaceService.removeFavorite(templateId, String.valueOf(userId));
        return Result.ok(Map.of("message", "已取消收藏"));
    }

    /**
     * 获取用户收藏的模板列表
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @param page 页码（默认1）
     * @param pageSize 每页数量（默认20）
     * @return 用户收藏的模板分页列表
     */
    // ========== 2.4.5 GET /api/marketplace/templates/favorites — 用户收藏列表 ==========
    @GetMapping("/templates/favorites")
    public Result<?> getFavorites(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        // C-04: userId 从 @AuthenticationPrincipal 获取
        // M-09: 通过 Service 获取收藏列表，而非直接操作 Mapper
        IPage<MarketplaceTemplate> result = marketplaceService.getFavorites(String.valueOf(userId), page, pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("page_size", pageSize);
        data.put("templates", result.getRecords());
        return Result.ok(data);
    }

    /**
     * 提交模板到市场审核
     * 用户将自己的SOP模板提交到市场供其他用户使用，需经过管理员审核
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @param req 提交请求，包含SOP ID、标题、描述、分类等信息
     * @return 操作结果
     */
    // ========== 2.4.6 POST /api/marketplace/templates — 提交模板审核 ==========
    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<?> submitTemplate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MarketplaceTemplateSubmitRequest req) {
        try {
            // C-04: userId 从 @AuthenticationPrincipal 获取
            User user = userService.getUserById(userId);
            String userName = user != null ? user.getUsername() : "未知用户";
            marketplaceService.submitTemplate(
                    String.valueOf(userId), userName,
                    req.getSopId(), req.getTitle(),
                    req.getDescription(), req.getCategory(),
                    req.getSubCategory(), req.getCoverUrl());
            return Result.ok(Map.of("message", "提交成功，等待审核"));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 获取模板的评价列表
     * @param templateId 模板ID
     * @param page 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 评价分页列表
     */
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

    /**
     * 提交模板评价
     * 用户对使用过的模板进行评分和评论
     * @param templateId 模板ID
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @param req 评价请求，包含评分（1-5）和评论内容
     * @return 操作结果
     */
    // ========== 2.4.8 POST /api/marketplace/templates/:template_id/reviews — 提交评价 ==========
    @PostMapping("/templates/{templateId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<?> addReview(
            @PathVariable String templateId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MarketplaceReviewRequest req) {
        try {
            // C-04: userId 从 @AuthenticationPrincipal 获取
            String userName = "未知用户";
            User user = userService.getUserById(userId);
            if (user != null) userName = user.getUsername();
            marketplaceService.addReview(templateId, String.valueOf(userId), userName,
                    req.getRating(), req.getComment());
            return Result.ok(Map.of("message", "评价已提交"));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
