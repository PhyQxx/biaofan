package com.biaofan.service.impl;


/**
 * 游戏化管理服务实现
 * - 徽章管理：增删改查、启用禁用
 * - 积分规则管理
 * - 商品管理：增删改查、上下架
 * - 用户游戏化数据查询
 */
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.GamificationBadgeDefinition;
import com.biaofan.entity.GamificationGrowthRule;
import com.biaofan.entity.GamificationStoreProduct;
import com.biaofan.mapper.GamificationBadgeDefinitionMapper;
import com.biaofan.mapper.GamificationGrowthRuleMapper;
import com.biaofan.mapper.GamificationStoreProductMapper;
import com.biaofan.service.AdminGamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员积分化管理服务实现类
 * 提供徽章、商品、成长规则的增删改查管理功能
 * 供后台管理员配置积分化体系的各项元素
 *
 * @author biaofan
 */
@Service
@RequiredArgsConstructor
public class AdminGamificationServiceImpl implements AdminGamificationService {

    private final GamificationBadgeDefinitionMapper badgeMapper;
    private final GamificationStoreProductMapper productMapper;
    private final GamificationGrowthRuleMapper growthRuleMapper;

    // ========== Badge Management ==========

    /**
     * 获取所有徽章定义列表
     * @return 徽章列表
     */
    @Override
    public List<Map<String, Object>> listBadges() {
        List<GamificationBadgeDefinition> badges = badgeMapper.selectList(
            new LambdaQueryWrapper<GamificationBadgeDefinition>()
                .orderByDesc(GamificationBadgeDefinition::getId)
        );
        return badges.stream().map(this::badgeToMap).collect(Collectors.toList());
    }

    /**
     * 创建新徽章
     * @param badge 徽章实体
     */
    @Override
    public void createBadge(GamificationBadgeDefinition badge) {
        badge.setCreatedAt(LocalDateTime.now());
        badgeMapper.insert(badge);
    }

    /**
     * 更新徽章信息
     * @param id 徽章ID
     * @param badge 新的徽章信息
     */
    @Override
    public void updateBadge(Long id, GamificationBadgeDefinition badge) {
        GamificationBadgeDefinition existing = badgeMapper.selectById(id);
        if (existing == null) throw new RuntimeException("徽章不存在");
        existing.setBadgeKey(badge.getBadgeKey());
        existing.setName(badge.getName());
        existing.setIcon(badge.getIcon());
        existing.setRarity(badge.getRarity());
        existing.setDescription(badge.getDescription());
        existing.setCondition(badge.getCondition());
        existing.setRewardExp(badge.getRewardExp());
        existing.setRewardScore(badge.getRewardScore());
        badgeMapper.updateById(existing);
    }

    /**
     * 删除徽章
     * @param id 徽章ID
     */
    @Override
    public void deleteBadge(Long id) {
        badgeMapper.deleteById(id);
    }

    /**
     * 将徽章实体转换为Map
     */
    private Map<String, Object> badgeToMap(GamificationBadgeDefinition badge) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", badge.getId());
        map.put("badgeKey", badge.getBadgeKey());
        map.put("name", badge.getName());
        map.put("icon", badge.getIcon());
        map.put("rarity", badge.getRarity());
        map.put("description", badge.getDescription());
        map.put("condition", badge.getCondition());
        map.put("rewardExp", badge.getRewardExp());
        map.put("rewardScore", badge.getRewardScore());
        map.put("createdAt", badge.getCreatedAt());
        return map;
    }

    // ========== Product Management ==========

    /**
     * 获取所有商品列表
     * @return 商品列表
     */
    @Override
    public List<Map<String, Object>> listProducts() {
        List<GamificationStoreProduct> products = productMapper.selectList(
            new LambdaQueryWrapper<GamificationStoreProduct>()
                .orderByDesc(GamificationStoreProduct::getId)
        );
        return products.stream().map(this::productToMap).collect(Collectors.toList());
    }

    /**
     * 创建新商品
     * @param product 商品实体
     */
    @Override
    public void createProduct(GamificationStoreProduct product) {
        product.setCreatedAt(LocalDateTime.now());
        productMapper.insert(product);
    }

    /**
     * 更新商品信息
     * @param id 商品ID
     * @param product 新的商品信息
     */
    @Override
    public void updateProduct(Long id, GamificationStoreProduct product) {
        GamificationStoreProduct existing = productMapper.selectById(id);
        if (existing == null) throw new RuntimeException("商品不存在");
        existing.setName(product.getName());
        existing.setIcon(product.getIcon());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setStock(product.getStock());
        existing.setActive(product.getActive());
        productMapper.updateById(existing);
    }

    /**
     * 删除商品
     * @param id 商品ID
     */
    @Override
    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
    }

    /**
     * 将商品实体转换为Map
     */
    private Map<String, Object> productToMap(GamificationStoreProduct product) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", product.getId());
        map.put("name", product.getName());
        map.put("icon", product.getIcon());
        map.put("description", product.getDescription());
        map.put("price", product.getPrice());
        map.put("stock", product.getStock());
        map.put("active", product.getActive());
        map.put("createdAt", product.getCreatedAt());
        return map;
    }

    // ========== Growth Rules ==========

    /**
     * 获取成长规则列表
     * @return 成长规则列表
     */
    @Override
    public List<GamificationGrowthRule> listGrowthRules() {
        return growthRuleMapper.selectList(
            new LambdaQueryWrapper<GamificationGrowthRule>()
                .orderByAsc(GamificationGrowthRule::getRuleType)
        );
    }

    /**
     * 批量更新成长规则
     * @param rules 成长规则列表
     */
    @Override
    @Transactional
    public void updateGrowthRules(List<GamificationGrowthRule> rules) {
        for (GamificationGrowthRule rule : rules) {
            if (rule.getId() != null) {
                growthRuleMapper.updateById(rule);
            } else {
                rule.setCreatedAt(LocalDateTime.now());
                growthRuleMapper.insert(rule);
            }
        }
    }
}
