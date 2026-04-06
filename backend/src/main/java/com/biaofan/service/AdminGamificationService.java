package com.biaofan.service;

import com.biaofan.entity.GamificationBadgeDefinition;
import com.biaofan.entity.GamificationGrowthRule;
import com.biaofan.entity.GamificationStoreProduct;
import java.util.List;
import java.util.Map;

public interface AdminGamificationService {
    // Badge management
    List<Map<String, Object>> listBadges();
    void createBadge(GamificationBadgeDefinition badge);
    void updateBadge(Long id, GamificationBadgeDefinition badge);
    void deleteBadge(Long id);

    // Product management
    List<Map<String, Object>> listProducts();
    void createProduct(GamificationStoreProduct product);
    void updateProduct(Long id, GamificationStoreProduct product);
    void deleteProduct(Long id);

    // Growth rules
    List<GamificationGrowthRule> listGrowthRules();
    void updateGrowthRules(List<GamificationGrowthRule> rules);
}
