package com.biaofan.controller;

import com.biaofan.dto.DashboardStatsDTO;
import com.biaofan.service.DashboardStatsService;
import com.biaofan.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仪表盘统计控制器
 * 提供仪表盘数据、完成率趋势、排行榜等统计功能
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class DashboardStatsController {

    private final DashboardStatsService dashboardStatsService;

    /**
     * 获取仪表盘统计数据
     * 包含今日概况、完成率趋势、TOP成员排名等
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 仪表盘统计数据
     */
    @GetMapping("/dashboard")
    public Result<DashboardStatsDTO> dashboard(@AuthenticationPrincipal Long userId) {
        return Result.ok(dashboardStatsService.getDashboardStats(userId));
    }

    /**
     * 获取完成率趋势
     * @param days 统计天数（默认7天）
     * @return 每日完成率趋势数据列表
     */
    @GetMapping("/trend")
    public Result<List<DashboardStatsDTO.TrendPoint>> trend(
            @RequestParam(defaultValue = "7") int days) {
        return Result.ok(dashboardStatsService.getTrend(days));
    }

    /**
     * 获取执行人排行榜
     * @param limit 返回数量限制（默认10）
     * @return 成员完成情况排行榜
     */
    @GetMapping("/leaderboard")
    public Result<List<DashboardStatsDTO.MemberStat>> leaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(dashboardStatsService.getLeaderboard(limit));
    }
}
