package com.biaofan.service;

import com.biaofan.entity.GamificationBadgeDefinition;
import com.biaofan.entity.GamificationGrowthRule;
import com.biaofan.entity.GamificationStoreProduct;
import java.util.List;
import java.util.Map;

/**
 * 管理后台积分与成就服务接口
 * 提供徽章、商品、成长规则等后台管理功能
 */
public interface AdminGamificationService {
    /**
     * 获取徽章定义列表
     * @return 徽章列表
     */
    List<Map<String, Object>> listBadges();

    /**
     * 创建徽章
     * @param badge 徽章定义实体
     */
    void createBadge(GamificationBadgeDefinition badge);

    /**
     * 更新徽章
     * @param id 徽章ID
     * @param badge 徽章定义实体
     */
    void updateBadge(Long id, GamificationBadgeDefinition badge);

    /**
     * 删除徽章
     * @param id 徽章ID
     */
    void deleteBadge(Long id);

    /**
     * 获取商品列表
     * @return 商品列表
     */
    List<Map<String, Object>> listProducts();

    /**
     * 创建商品
     * @param product 商品实体
     */
    void createProduct(GamificationStoreProduct product);

    /**
     * 更新商品
     * @param id 商品ID
     * @param product 商品实体
     */
    void updateProduct(Long id, GamificationStoreProduct product);

    /**
     * 删除商品
     * @param id 商品ID
     */
    void deleteProduct(Long id);

    /**
     * 获取成长规则列表
     * @return 成长规则列表
     */
    List<GamificationGrowthRule> listGrowthRules();

    /**
     * 更新成长规则
     * @param rules 成长规则列表
     */
    void updateGrowthRules(List<GamificationGrowthRule> rules);
}
