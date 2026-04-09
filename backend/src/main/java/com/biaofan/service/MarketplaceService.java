package com.biaofan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.biaofan.entity.MarketplaceTemplate;

public interface MarketplaceService {
    // Template browsing
    IPage<MarketplaceTemplate> getTemplateList(String category, String keyword, String sort, int page, int pageSize);
    MarketplaceTemplate getTemplateDetail(String templateId, String userId);

    // Template usage
    Long useTemplate(String templateId, String userId, String sopName);

    // Favorites
    void addFavorite(String templateId, String userId);
    void removeFavorite(String templateId, String userId);
    boolean isFavorited(String templateId, String userId);
    IPage<MarketplaceTemplate> getFavorites(String userId, int page, int pageSize);

    // Submit template for review
    void submitTemplate(String userId, String userName, String sopId, String title, String description, String category, String subCategory, String coverUrl);

    // Reviews
    IPage getReviews(String templateId, int page, int pageSize);
    void addReview(String templateId, String userId, String userName, Integer rating, String comment);

    // Admin
    IPage<MarketplaceTemplate> getAuditList(String status, int page, int pageSize);
    void auditTemplate(String templateId, String auditorId, String status, String rejectReason);
    void offlineTemplate(String templateId);
}
