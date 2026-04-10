package com.biaofan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.biaofan.entity.MarketplaceTemplate;

/**
 * 模板市场服务接口
 * 提供SOP模板的浏览、使用、收藏、评价、审核等功能
 */
public interface MarketplaceService {
    /**
     * 获取模板列表（分页）
     * @param category 分类（可为空）
     * @param keyword 关键词搜索（可为空）
     * @param sort 排序规则（如latest、popular）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 模板分页列表
     */
    IPage<MarketplaceTemplate> getTemplateList(String category, String keyword, String sort, int page, int pageSize);

    /**
     * 获取模板详情
     * @param templateId 模板ID
     * @param userId 当前用户ID（用于判断是否已收藏）
     * @return 模板详情
     */
    MarketplaceTemplate getTemplateDetail(String templateId, String userId);

    /**
     * 使用模板创建我的SOP
     * @param templateId 模板ID
     * @param userId 用户ID
     * @param sopName 自定义的SOP名称
     * @return 创建的SOP ID
     */
    Long useTemplate(String templateId, String userId, String sopName);

    /**
     * 添加收藏
     * @param templateId 模板ID
     * @param userId 用户ID
     */
    void addFavorite(String templateId, String userId);

    /**
     * 取消收藏
     * @param templateId 模板ID
     * @param userId 用户ID
     */
    void removeFavorite(String templateId, String userId);

    /**
     * 判断是否已收藏
     * @param templateId 模板ID
     * @param userId 用户ID
     * @return 是否已收藏
     */
    boolean isFavorited(String templateId, String userId);

    /**
     * 获取我的收藏列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 收藏的模板分页列表
     */
    IPage<MarketplaceTemplate> getFavorites(String userId, int page, int pageSize);

    /**
     * 提交模板待审核
     * @param userId 用户ID
     * @param userName 用户名称
     * @param sopId 关联的SOP ID
     * @param title 模板标题
     * @param description 模板描述
     * @param category 分类
     * @param subCategory 子分类
     * @param coverUrl 封面图URL
     */
    void submitTemplate(String userId, String userName, String sopId, String title, String description, String category, String subCategory, String coverUrl);

    /**
     * 获取模板评价列表
     * @param templateId 模板ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 评价分页列表
     */
    IPage getReviews(String templateId, int page, int pageSize);

    /**
     * 添加模板评价
     * @param templateId 模板ID
     * @param userId 用户ID
     * @param userName 用户名称
     * @param rating 评分（1-5）
     * @param comment 评价内容
     */
    void addReview(String templateId, String userId, String userName, Integer rating, String comment);

    /**
     * 获取待审核模板列表（管理员）
     * @param status 审核状态（如pending、approved、rejected）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 模板分页列表
     */
    IPage<MarketplaceTemplate> getAuditList(String status, int page, int pageSize);

    /**
     * 审核模板（管理员）
     * @param templateId 模板ID
     * @param auditorId 审核员ID
     * @param status 审核状态（approved/rejected）
     * @param rejectReason 拒绝原因（通过时为空）
     */
    void auditTemplate(String templateId, String auditorId, String status, String rejectReason);

    /**
     * 下线模板（管理员）
     * @param templateId 模板ID
     */
    void offlineTemplate(String templateId);
}
