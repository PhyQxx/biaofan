package com.biaofan.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.dto.Result;
import com.biaofan.service.GamificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 成长体系控制器
 * 提供用户积分、徽章、排行榜、兑换商城等Gamification功能
 */
@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    /**
     * 获取用户成长体系档案
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 用户等级、积分、称号等信息
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(@AuthenticationPrincipal Long userId) {
        return Result.ok(gamificationService.getProfile(userId));
    }

    /**
     * 获取用户已获得的徽章列表
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 徽章列表
     */
    @GetMapping("/badges")
    public Result<List<Map<String, Object>>> badges(@AuthenticationPrincipal Long userId) {
        return Result.ok(gamificationService.getBadges(userId));
    }

    /**
     * 获取徽章详情
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @param badgeId 徽章ID
     * @return 徽章详细信息
     */
    @GetMapping("/badges/{badgeId}")
    public Result<Map<String, Object>> badgeDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long badgeId) {
        return Result.ok(gamificationService.getBadgeDetail(userId, badgeId));
    }

    /**
     * 获取积分排行榜
     * @param period 周期：day、week、month、all（可选）
     * @return 排行榜列表
     */
    @GetMapping("/leaderboard")
    public Result<List<Map<String, Object>>> leaderboard(
            @RequestParam(required = false) String period) {
        return Result.ok(gamificationService.getLeaderboard(period));
    }

    /**
     * 获取排行榜概览
     * @return 当前周期冠军及统计数据
     */
    @GetMapping("/leaderboard/overview")
    public Result<Map<String, Object>> leaderboardOverview() {
        return Result.ok(gamificationService.getLeaderboardOverview());
    }

    /**
     * 获取用户当前积分
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 积分信息
     */
    @GetMapping("/score")
    public Result<Map<String, Object>> score(@AuthenticationPrincipal Long userId) {
        return Result.ok(gamificationService.getScore(userId));
    }

    /**
     * 获取积分变动历史
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @param page 页码（默认1）
     * @param size 每页数量（默认20）
     * @return 积分历史记录
     */
    @GetMapping("/score/history")
    public Result<List<Map<String, Object>>> scoreHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(gamificationService.getScoreHistory(userId, page, size));
    }

    /**
     * 兑换商品
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @param body 请求体，包含商品ID
     * @return 兑换结果
     */
    @PostMapping("/score/redeem")
    public Result<Map<String, Object>> redeem(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody Map<String, Long> body) {
        try {
            return Result.ok(gamificationService.redeemProduct(userId, body.get("productId")));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取积分商城商品列表
     * @param page 页码（默认1）
     * @param size 每页数量（默认20）
     * @return 可兑换商品列表
     */
    @GetMapping("/store")
    public Result<Map<String, Object>> store(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(gamificationService.getStore(page, size));
    }

    /**
     * 获取用户成长进度
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 当前等级进度、下一等级所需积分等
     */
    @GetMapping("/progress")
    public Result<Map<String, Object>> progress(@AuthenticationPrincipal Long userId) {
        return Result.ok(gamificationService.getProgress(userId));
    }

    /**
     * 获取等级排行榜
     * @return 按等级排序的用户排名
     */
    @GetMapping("/level-ranking")
    public Result<List<Map<String, Object>>> levelRanking() {
        return Result.ok(gamificationService.getLevelRanking());
    }
}
