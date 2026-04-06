package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.GamificationBadgeDefinition;
import com.biaofan.entity.GamificationGrowthRule;
import com.biaofan.entity.GamificationStoreProduct;
import com.biaofan.service.AdminGamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/gamification")
@RequiredArgsConstructor
public class AdminGamificationController {

    private final AdminGamificationService adminGamificationService;

    // ========== Badge Management ==========

    @GetMapping("/badges")
    public Result<Map<String, Object>> listBadges() {
        return Result.ok(Map.of(
            "list", adminGamificationService.listBadges(),
            "total", adminGamificationService.listBadges().size()
        ));
    }

    @PostMapping("/badges")
    public Result<Void> createBadge(@RequestBody GamificationBadgeDefinition badge) {
        if (badge.getName() == null || badge.getName().isBlank()) {
            return Result.fail("徽章名称不能为空");
        }
        adminGamificationService.createBadge(badge);
        return Result.ok();
    }

    @PutMapping("/badges/{id}")
    public Result<Void> updateBadge(@PathVariable Long id, @RequestBody GamificationBadgeDefinition badge) {
        adminGamificationService.updateBadge(id, badge);
        return Result.ok();
    }

    @DeleteMapping("/badges/{id}")
    public Result<Void> deleteBadge(@PathVariable Long id) {
        adminGamificationService.deleteBadge(id);
        return Result.ok();
    }

    // ========== Product Management ==========

    @GetMapping("/products")
    public Result<Map<String, Object>> listProducts() {
        return Result.ok(Map.of(
            "list", adminGamificationService.listProducts(),
            "total", adminGamificationService.listProducts().size()
        ));
    }

    @PostMapping("/products")
    public Result<Void> createProduct(@RequestBody GamificationStoreProduct product) {
        adminGamificationService.createProduct(product);
        return Result.ok();
    }

    @PutMapping("/products/{id}")
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody GamificationStoreProduct product) {
        adminGamificationService.updateProduct(id, product);
        return Result.ok();
    }

    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        adminGamificationService.deleteProduct(id);
        return Result.ok();
    }

    // ========== Growth Rules ==========

    @GetMapping("/rules")
    public Result<List<GamificationGrowthRule>> listGrowthRules() {
        return Result.ok(adminGamificationService.listGrowthRules());
    }

    @PutMapping("/rules")
    public Result<Void> updateGrowthRules(@RequestBody List<GamificationGrowthRule> rules) {
        adminGamificationService.updateGrowthRules(rules);
        return Result.ok();
    }
}
