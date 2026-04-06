package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(@AuthenticationPrincipal Long userId) {
        return Result.ok(gamificationService.getProfile(userId));
    }

    @GetMapping("/badges")
    public Result<List<Map<String, Object>>> badges(@AuthenticationPrincipal Long userId) {
        return Result.ok(gamificationService.getBadges(userId));
    }

    @GetMapping("/badges/{badgeId}")
    public Result<Map<String, Object>> badgeDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long badgeId) {
        return Result.ok(gamificationService.getBadgeDetail(userId, badgeId));
    }

    @GetMapping("/leaderboard")
    public Result<List<Map<String, Object>>> leaderboard(
            @RequestParam(required = false) String period) {
        return Result.ok(gamificationService.getLeaderboard(period));
    }

    @GetMapping("/leaderboard/overview")
    public Result<Map<String, Object>> leaderboardOverview() {
        return Result.ok(gamificationService.getLeaderboardOverview());
    }

    @GetMapping("/score")
    public Result<Map<String, Object>> score(@AuthenticationPrincipal Long userId) {
        return Result.ok(gamificationService.getScore(userId));
    }

    @GetMapping("/score/history")
    public Result<List<Map<String, Object>>> scoreHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(gamificationService.getScoreHistory(userId, page, size));
    }

    @PostMapping("/score/redeem")
    public Result<Map<String, Object>> redeem(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Long> body) {
        try {
            return Result.ok(gamificationService.redeemProduct(userId, body.get("productId")));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/store")
    public Result<List<Map<String, Object>>> store() {
        return Result.ok(gamificationService.getStore());
    }

    @GetMapping("/progress")
    public Result<Map<String, Object>> progress(@AuthenticationPrincipal Long userId) {
        return Result.ok(gamificationService.getProgress(userId));
    }

    @GetMapping("/level-ranking")
    public Result<List<Map<String, Object>>> levelRanking() {
        return Result.ok(gamificationService.getLevelRanking());
    }
}
