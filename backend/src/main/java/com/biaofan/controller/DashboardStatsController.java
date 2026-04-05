package com.biaofan.controller;

import com.biaofan.dto.DashboardStatsDTO;
import com.biaofan.service.DashboardStatsService;
import com.biaofan.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class DashboardStatsController {

    private final DashboardStatsService dashboardStatsService;

    /**
     * GET /api/stats/dashboard — 获取仪表盘统计数据
     * 包含今日概况、完成率趋势、TOP成员排名
     */
    @GetMapping("/dashboard")
    public Result<DashboardStatsDTO> dashboard(@AuthenticationPrincipal Long userId) {
        return Result.ok(dashboardStatsService.getDashboardStats(userId));
    }

    /**
     * GET /api/stats/trend?days=7 — 完成率趋势
     */
    @GetMapping("/trend")
    public Result<List<DashboardStatsDTO.TrendPoint>> trend(
            @RequestParam(defaultValue = "7") int days) {
        return Result.ok(dashboardStatsService.getTrend(days));
    }

    /**
     * GET /api/stats/leaderboard — 执行人排行榜
     */
    @GetMapping("/leaderboard")
    public Result<List<DashboardStatsDTO.MemberStat>> leaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(dashboardStatsService.getLeaderboard(limit));
    }
}
