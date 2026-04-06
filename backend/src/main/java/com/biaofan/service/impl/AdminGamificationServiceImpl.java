package com.biaofan.service.impl;

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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminGamificationServiceImpl implements AdminGamificationService {

    private final GamificationBadgeDefinitionMapper badgeMapper;
    private final GamificationStoreProductMapper productMapper;
    private final GamificationGrowthRuleMapper growthRuleMapper;

    // ========== Badge Management ==========

    @Override
    public List<Map<String, Object>> listBadges() {
        List<GamificationBadgeDefinition> badges = badgeMapper.selectList(
            new LambdaQueryWrapper<GamificationBadgeDefinition>()
                .orderByDesc(GamificationBadgeDefinition::getId)
        );
        return badges.stream().map(this::badgeToMap).collect(Collectors.toList());
    }

    @Override
    public void createBadge(GamificationBadgeDefinition badge) {
        badge.setCreatedAt(LocalDateTime.now());
        badgeMapper.insert(badge);
    }

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

    @Override
    public void deleteBadge(Long id) {
        badgeMapper.deleteById(id);
    }

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

    @Override
    public List<Map<String, Object>> listProducts() {
        List<GamificationStoreProduct> products = productMapper.selectList(
            new LambdaQueryWrapper<GamificationStoreProduct>()
                .orderByDesc(GamificationStoreProduct::getId)
        );
        return products.stream().map(this::productToMap).collect(Collectors.toList());
    }

    @Override
    public void createProduct(GamificationStoreProduct product) {
        product.setCreatedAt(LocalDateTime.now());
        productMapper.insert(product);
    }

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

    @Override
    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
    }

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

    @Override
    public List<GamificationGrowthRule> listGrowthRules() {
        return growthRuleMapper.selectList(
            new LambdaQueryWrapper<GamificationGrowthRule>()
                .orderByAsc(GamificationGrowthRule::getRuleType)
        );
    }

    @Override
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
