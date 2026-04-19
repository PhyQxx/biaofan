package com.biaofan.service.impl;


/**
 * 模板市场服务实现
 * - 模板列表查询（分类、关键词、排序）
 * - 模板详情
 * - 模板提交审核
 * - 模板使用（创建执行单）
 * - 收藏 / 取消收藏
 * - 评分评论
 */
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.MarketplaceFavorite;
import com.biaofan.entity.MarketplaceReview;
import com.biaofan.entity.MarketplaceTemplate;
import com.biaofan.entity.Sop;
import com.biaofan.mapper.MarketplaceFavoriteMapper;
import com.biaofan.mapper.MarketplaceReviewMapper;
import com.biaofan.mapper.MarketplaceTemplateMapper;
import com.biaofan.mapper.SopMapper;
import com.biaofan.service.MarketplaceService;
import com.biaofan.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模板市场服务实现类
 * 提供SOP模板的浏览、搜索、使用、收藏、评价、提交审核及管理功能
 * 支持按热度/评分/时间排序，模板使用后自动增加使用计数
 *
 * @author biaofan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceServiceImpl implements MarketplaceService {

    private final MarketplaceTemplateMapper templateMapper;
    private final MarketplaceReviewMapper reviewMapper;
    private final MarketplaceFavoriteMapper favoriteMapper;
    private final SopMapper sopMapper;

    /**
     * 获取模板市场列表
     * @param category 分类筛选
     * @param keyword 关键词搜索（标题/描述）
     * @param sort 排序方式：popular/rated/newest
     * @param page 页码
     * @param pageSize 每页数量
     * @return 模板分页列表
     */
    @Override
    @Cacheable(value = "marketplaceTemplates", key = "#category + '_' + #keyword + '_' + #sort + '_' + #page + '_' + #pageSize")
    public IPage<MarketplaceTemplate> getTemplateList(String category, String keyword, String sort, int page, int pageSize) {
        Page<MarketplaceTemplate> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<MarketplaceTemplate> q = new LambdaQueryWrapper<MarketplaceTemplate>()
                .eq(MarketplaceTemplate::getStatus, "approved");
        if (category != null && !category.isEmpty()) {
            q.eq(MarketplaceTemplate::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            q.and(w -> w.like(MarketplaceTemplate::getTitle, keyword)
                    .or().like(MarketplaceTemplate::getDescription, keyword));
        }
        if ("popular".equals(sort)) {
            q.orderByDesc(MarketplaceTemplate::getUseCount);
        } else if ("rated".equals(sort)) {
            q.orderByDesc(MarketplaceTemplate::getAvgRating);
        } else {
            q.orderByDesc(MarketplaceTemplate::getCreatedAt);
        }
        return templateMapper.selectPage(p, q);
    }

    /**
     * 获取模板详情
     * @param templateId 模板ID
     * @param userId 当前用户ID
     * @return 模板详情
     */
    @Override
    public MarketplaceTemplate getTemplateDetail(String templateId, String userId) {
        MarketplaceTemplate t = templateMapper.selectOne(
                new LambdaQueryWrapper<MarketplaceTemplate>()
                        .eq(MarketplaceTemplate::getTemplateId, templateId));
        if (t == null) throw new RuntimeException("模板不存在");
        return t;
    }

    /**
     * 使用模板创建SOP
     * 将模板内容复制为用户的新SOP草稿
     * @param templateId 模板ID
     * @param userId 用户ID
     * @param sopName 自定义的SOP名称
     * @return 新创建的SOP ID
     */
    @Override
    @Transactional
    public Long useTemplate(String templateId, String userId, String sopName) {
        MarketplaceTemplate tpl = templateMapper.selectOne(
                new LambdaQueryWrapper<MarketplaceTemplate>()
                        .eq(MarketplaceTemplate::getTemplateId, templateId));
        if (tpl == null) throw new RuntimeException("模板不存在");
        if (!"approved".equals(tpl.getStatus())) throw new RuntimeException("该模板未上架，无法使用");

        // Copy SOP as draft
        Sop source = sopMapper.selectById(tpl.getSopId());
        if (source == null) throw new RuntimeException("关联的SOP不存在");

        Sop copy = new Sop();
        copy.setTitle(sopName != null && !sopName.isEmpty() ? sopName : tpl.getTitle());
        copy.setDescription(source.getDescription());
        copy.setContent(source.getContent());
        copy.setCategory(source.getCategory());
        copy.setTags(source.getTags());
        copy.setVersion(1);
        copy.setStatus("draft");
        copy.setUserId(Long.parseLong(userId));
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());
        sopMapper.insert(copy);

        // Increment use count
        templateMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MarketplaceTemplate>()
                        .eq(MarketplaceTemplate::getTemplateId, templateId)
                        .setSql("use_count = use_count + 1"));

        return copy.getId();
    }

    /**
     * 添加模板收藏
     * @param templateId 模板ID
     * @param userId 用户ID
     */
    @Override
    public void addFavorite(String templateId, String userId) {
        // Use insertIgnore to prevent TOCTOU race condition
        MarketplaceFavorite fav = new MarketplaceFavorite();
        fav.setUserId(userId);
        fav.setTemplateId(templateId);
        fav.setCreatedAt(LocalDateTime.now());
        int inserted = favoriteMapper.insertIgnore(fav);
        if (inserted > 0) {
            log.info("收藏成功 userId={}, templateId={}", userId, templateId);
        }
    }

    /**
     * 取消模板收藏
     * @param templateId 模板ID
     * @param userId 用户ID
     */
    @Override
    public void removeFavorite(String templateId, String userId) {
        favoriteMapper.delete(new LambdaQueryWrapper<MarketplaceFavorite>()
                .eq(MarketplaceFavorite::getUserId, userId)
                .eq(MarketplaceFavorite::getTemplateId, templateId));
    }

    /**
     * 检查模板是否已收藏
     * @param templateId 模板ID
     * @param userId 用户ID
     * @return 是否已收藏
     */
    @Override
    public boolean isFavorited(String templateId, String userId) {
        return favoriteMapper.selectOne(
                new LambdaQueryWrapper<MarketplaceFavorite>()
                        .eq(MarketplaceFavorite::getUserId, userId)
                        .eq(MarketplaceFavorite::getTemplateId, templateId)) != null;
    }

    /**
     * 获取用户的收藏模板列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 收藏模板分页列表
     */
    @Override
    public IPage<MarketplaceTemplate> getFavorites(String userId, int page, int pageSize) {
        Page<MarketplaceFavorite> favPage = new Page<>(page, pageSize);
        IPage<MarketplaceFavorite> favs = favoriteMapper.selectPage(favPage,
                new LambdaQueryWrapper<MarketplaceFavorite>()
                        .eq(MarketplaceFavorite::getUserId, userId)
                        .orderByDesc(MarketplaceFavorite::getCreatedAt));

        if (favs.getRecords().isEmpty()) {
            Page<MarketplaceTemplate> empty = new Page<>(page, pageSize);
            empty.setTotal(0);
            return empty;
        }

        var templateIds = favs.getRecords().stream()
                .map(MarketplaceFavorite::getTemplateId)
                .collect(Collectors.toList());

        Page<MarketplaceTemplate> result = new Page<>(page, pageSize);
        result.setTotal(favs.getTotal());
        var list = templateMapper.selectList(
                new LambdaQueryWrapper<MarketplaceTemplate>()
                        .in(MarketplaceTemplate::getTemplateId, templateIds)
                        .eq(MarketplaceTemplate::getStatus, "approved")
                        .orderByDesc(MarketplaceTemplate::getTemplateId)
                        .last("LIMIT " + pageSize + " OFFSET " + ((page - 1) * pageSize)));
        result.setRecords(list);
        return result;
    }

    /**
     * 提交SOP为市场模板（待审核）
     * @param userId 用户ID
     * @param userName 用户名
     * @param sopId 源SOP ID
     * @param title 模板标题
     * @param description 模板描述
     * @param category 分类
     * @param subCategory 子分类
     * @param coverUrl 封面图URL
     */
    @Override
    @Transactional
    public void submitTemplate(String userId, String userName, String sopId, String title,
                               String description, String category, String subCategory, String coverUrl) {
        Sop sop = sopMapper.selectById(sopId);
        if (sop == null) throw new RuntimeException("SOP不存在");
        if (!sop.getUserId().toString().equals(userId)) throw new RuntimeException("只能提交自己的SOP");

        MarketplaceTemplate tpl = new MarketplaceTemplate();
        tpl.setTemplateId(IdUtil.generateTemplateId());
        tpl.setSopId(sopId);
        tpl.setTitle(title);
        tpl.setDescription(description);
        tpl.setCategory(category != null ? category : "other");
        tpl.setSubCategory(subCategory);
        tpl.setCoverUrl(coverUrl);
        tpl.setAuthorId(userId);
        tpl.setAuthorName(userName);
        tpl.setStepCount(0);
        tpl.setAvgRating(BigDecimal.ZERO);
        tpl.setRatingCount(0);
        tpl.setUseCount(0);
        tpl.setStatus("pending");
        tpl.setCreatedAt(LocalDateTime.now());
        tpl.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(tpl);
    }

    /**
     * 获取模板的评价列表
     * @param templateId 模板ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 评价分页列表
     */
    @Override
    public IPage<MarketplaceReview> getReviews(String templateId, int page, int pageSize) {
        Page<MarketplaceReview> p = new Page<>(page, pageSize);
        return reviewMapper.selectPage(p,
                new LambdaQueryWrapper<MarketplaceReview>()
                        .eq(MarketplaceReview::getTemplateId, templateId)
                        .orderByDesc(MarketplaceReview::getCreatedAt));
    }

    /**
     * 添加或更新模板评价（幂等）
     * @param templateId 模板ID
     * @param userId 用户ID
     * @param userName 用户名
     * @param rating 评分
     * @param comment 评价内容
     */
    @Override
    @Transactional
    public void addReview(String templateId, String userId, String userName, Integer rating, String comment) {
        MarketplaceTemplate tpl = templateMapper.selectOne(
                new LambdaQueryWrapper<MarketplaceTemplate>()
                        .eq(MarketplaceTemplate::getTemplateId, templateId));
        if (tpl == null) throw new RuntimeException("模板不存在");

        // Upsert review
        MarketplaceReview existing = reviewMapper.selectOne(
                new LambdaQueryWrapper<MarketplaceReview>()
                        .eq(MarketplaceReview::getTemplateId, templateId)
                        .eq(MarketplaceReview::getUserId, userId));
        if (existing != null) {
            existing.setRating(rating);
            existing.setComment(comment);
            reviewMapper.updateById(existing);
        } else {
            MarketplaceReview review = new MarketplaceReview();
            review.setTemplateId(templateId);
            review.setUserId(userId);
            review.setUserName(userName);
            review.setRating(rating);
            review.setComment(comment);
            review.setCreatedAt(LocalDateTime.now());
            reviewMapper.insert(review);
        }

        // Recalculate avg rating
        Double avg = reviewMapper.selectAvgRating(templateId);
        Integer count = reviewMapper.selectReviewCount(templateId);
        templateMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MarketplaceTemplate>()
                        .eq(MarketplaceTemplate::getTemplateId, templateId)
                        .set(MarketplaceTemplate::getAvgRating, avg != null ? BigDecimal.valueOf(avg) : BigDecimal.ZERO)
                        .set(MarketplaceTemplate::getRatingCount, count != null ? count : 0));
    }

    /**
     * 获取待审核模板列表（管理员）
     * @param status 状态筛选
     * @param page 页码
     * @param pageSize 每页数量
     * @return 模板分页列表
     */
    @Override
    public IPage<MarketplaceTemplate> getAuditList(String status, int page, int pageSize) {
        Page<MarketplaceTemplate> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<MarketplaceTemplate> q = new LambdaQueryWrapper<MarketplaceTemplate>();
        if (status != null && !status.isEmpty()) {
            q.eq(MarketplaceTemplate::getStatus, status);
        }
        q.orderByDesc(MarketplaceTemplate::getCreatedAt);
        return templateMapper.selectPage(p, q);
    }

    /**
     * 审核模板（管理员）
     * @param templateId 模板ID
     * @param auditorId 审核员ID
     * @param status 审核状态：approved/rejected
     * @param rejectReason 拒绝原因
     */
    @Override
    @Transactional
    public void auditTemplate(String templateId, String auditorId, String status, String rejectReason) {
        MarketplaceTemplate tpl = templateMapper.selectOne(
                new LambdaQueryWrapper<MarketplaceTemplate>()
                        .eq(MarketplaceTemplate::getTemplateId, templateId));
        if (tpl == null) throw new RuntimeException("模板不存在");

        // Prevent self-approval
        if (tpl.getAuthorId() != null && tpl.getAuthorId().equals(Long.valueOf(auditorId))) {
            throw new RuntimeException("不能审核自己的模板");
        }

        templateMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MarketplaceTemplate>()
                        .eq(MarketplaceTemplate::getTemplateId, templateId)
                        .set(MarketplaceTemplate::getStatus, status)
                        .set(MarketplaceTemplate::getRejectReason, rejectReason)
                        .set(MarketplaceTemplate::getAuditedBy, auditorId)
                        .set(MarketplaceTemplate::getAuditedAt, LocalDateTime.now()));
    }

    /**
     * 下架模板
     * @param templateId 模板ID
     */
    @Override
    @Transactional
    public void offlineTemplate(String templateId) {
        templateMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MarketplaceTemplate>()
                        .eq(MarketplaceTemplate::getTemplateId, templateId)
                        .set(MarketplaceTemplate::getStatus, "offline"));
    }
}
