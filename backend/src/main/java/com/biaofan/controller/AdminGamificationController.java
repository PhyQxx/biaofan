package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.GamificationBadgeDefinition;
import com.biaofan.entity.GamificationGrowthRule;
import com.biaofan.entity.GamificationStoreProduct;
import com.biaofan.service.AdminGamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员成长体系控制器
 * 提供管理员对徽章、商品、成长规则的管理功能
 */
@RestController
@RequestMapping("/api/admin/gamification")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminGamificationController {

    private final AdminGamificationService adminGamificationService;

    // ========== Badge Management ==========

    /**
     * 获取所有徽章定义列表
     * @return 徽章列表及总数
     */
    @GetMapping("/badges")
    public Result<Map<String, Object>> listBadges() {
        var badges = adminGamificationService.listBadges();
        return Result.ok(Map.of("list", badges, "total", badges.size()));
    }

    /**
     * 创建徽章
     * @param badge 徽章定义信息
     * @return 操作结果
     */
    @PostMapping("/badges")
    public Result<Void> createBadge(@RequestBody GamificationBadgeDefinition badge) {
        if (badge.getName() == null || badge.getName().isBlank()) {
            return Result.fail("徽章名称不能为空");
        }
        adminGamificationService.createBadge(badge);
        return Result.ok();
    }

    /**
     * 更新徽章
     * @param id 徽章ID
     * @param badge 徽章更新信息
     * @return 操作结果
     */
    @PutMapping("/badges/{id}")
    public Result<Void> updateBadge(@PathVariable Long id, @RequestBody GamificationBadgeDefinition badge) {
        adminGamificationService.updateBadge(id, badge);
        return Result.ok();
    }

    /**
     * 删除徽章
     * @param id 徽章ID
     * @return 操作结果
     */
    @DeleteMapping("/badges/{id}")
    public Result<Void> deleteBadge(@PathVariable Long id) {
        adminGamificationService.deleteBadge(id);
        return Result.ok();
    }

    // ========== Product Management ==========

    /**
     * 获取所有商城商品列表
     * @return 商品列表及总数
     */
    @GetMapping("/products")
    public Result<Map<String, Object>> listProducts() {
        return Result.ok(Map.of(
            "list", adminGamificationService.listProducts(),
            "total", adminGamificationService.listProducts().size()
        ));
    }

    /**
     * 创建商城商品
     * @param product 商品信息
     * @return 操作结果
     */
    @PostMapping("/products")
    public Result<Void> createProduct(@RequestBody GamificationStoreProduct product) {
        adminGamificationService.createProduct(product);
        return Result.ok();
    }

    /**
     * 更新商城商品
     * @param id 商品ID
     * @param product 商品更新信息
     * @return 操作结果
     */
    @PutMapping("/products/{id}")
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody GamificationStoreProduct product) {
        adminGamificationService.updateProduct(id, product);
        return Result.ok();
    }

    /**
     * 删除商城商品
     * @param id 商品ID
     * @return 操作结果
     */
    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        adminGamificationService.deleteProduct(id);
        return Result.ok();
    }

    // ========== Growth Rules ==========

    /**
     * 获取所有成长规则
     * @return 成长规则列表
     */
    @GetMapping("/rules")
    public Result<List<GamificationGrowthRule>> listGrowthRules() {
        return Result.ok(adminGamificationService.listGrowthRules());
    }

    /**
     * 更新成长规则
     * @param rules 成长规则列表
     * @return 操作结果
     */
    @PutMapping("/rules")
    public Result<Void> updateGrowthRules(@RequestBody List<GamificationGrowthRule> rules) {
        adminGamificationService.updateGrowthRules(rules);
        return Result.ok();
    }
}
