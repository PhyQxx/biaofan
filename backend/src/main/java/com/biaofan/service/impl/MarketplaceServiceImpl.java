package com.biaofan.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketplaceServiceImpl implements MarketplaceService {

    private final MarketplaceTemplateMapper templateMapper;
    private final MarketplaceReviewMapper reviewMapper;
    private final MarketplaceFavoriteMapper favoriteMapper;
    private final SopMapper sopMapper;

    @Override
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

    @Override
    public MarketplaceTemplate getTemplateDetail(String templateId, String userId) {
        MarketplaceTemplate t = templateMapper.selectOne(
                new LambdaQueryWrapper<MarketplaceTemplate>()
                        .eq(MarketplaceTemplate::getTemplateId, templateId));
        if (t == null) throw new RuntimeException("模板不存在");
        return t;
    }

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

    @Override
    public void addFavorite(String templateId, String userId) {
        MarketplaceFavorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<MarketplaceFavorite>()
                        .eq(MarketplaceFavorite::getUserId, userId)
                        .eq(MarketplaceFavorite::getTemplateId, templateId));
        if (existing != null) return;
        MarketplaceFavorite fav = new MarketplaceFavorite();
        fav.setUserId(userId);
        fav.setTemplateId(templateId);
        fav.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(fav);
    }

    @Override
    public void removeFavorite(String templateId, String userId) {
        favoriteMapper.delete(new LambdaQueryWrapper<MarketplaceFavorite>()
                .eq(MarketplaceFavorite::getUserId, userId)
                .eq(MarketplaceFavorite::getTemplateId, templateId));
    }

    @Override
    public boolean isFavorited(String templateId, String userId) {
        return favoriteMapper.selectOne(
                new LambdaQueryWrapper<MarketplaceFavorite>()
                        .eq(MarketplaceFavorite::getUserId, userId)
                        .eq(MarketplaceFavorite::getTemplateId, templateId)) != null;
    }

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
                        .eq(MarketplaceTemplate::getStatus, "approved"));
        result.setRecords(list);
        return result;
    }

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

    @Override
    public IPage<MarketplaceReview> getReviews(String templateId, int page, int pageSize) {
        Page<MarketplaceReview> p = new Page<>(page, pageSize);
        return reviewMapper.selectPage(p,
                new LambdaQueryWrapper<MarketplaceReview>()
                        .eq(MarketplaceReview::getTemplateId, templateId)
                        .orderByDesc(MarketplaceReview::getCreatedAt));
    }

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

    @Override
    @Transactional
    public void auditTemplate(String templateId, String auditorId, String status, String rejectReason) {
        MarketplaceTemplate tpl = templateMapper.selectOne(
                new LambdaQueryWrapper<MarketplaceTemplate>()
                        .eq(MarketplaceTemplate::getTemplateId, templateId));
        if (tpl == null) throw new RuntimeException("模板不存在");

        templateMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MarketplaceTemplate>()
                        .eq(MarketplaceTemplate::getTemplateId, templateId)
                        .set(MarketplaceTemplate::getStatus, status)
                        .set(MarketplaceTemplate::getRejectReason, rejectReason)
                        .set(MarketplaceTemplate::getAuditedBy, auditorId)
                        .set(MarketplaceTemplate::getAuditedAt, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void offlineTemplate(String templateId) {
        templateMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MarketplaceTemplate>()
                        .eq(MarketplaceTemplate::getTemplateId, templateId)
                        .set(MarketplaceTemplate::getStatus, "offline"));
    }
}
