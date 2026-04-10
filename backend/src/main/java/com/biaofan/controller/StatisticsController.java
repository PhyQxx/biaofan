package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.ExecutionStat;
import com.biaofan.service.ExecutionStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 执行统计控制器
 * 提供用户个人和全局的SOP执行统计数据
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final ExecutionStatService statService;

    /**
     * 获取当前用户的执行统计
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 用户执行统计数据列表
     */
    @GetMapping("/my")
    public Result<List<ExecutionStat>> myStats(@AuthenticationPrincipal Long userId) {
        return Result.ok(statService.getStats(userId));
    }

    /**
     * 获取全局执行统计
     * @return 全局执行统计数据列表
     */
    @GetMapping("/global")
    public Result<List<ExecutionStat>> globalStats() {
        return Result.ok(statService.getGlobalStats());
    }
}
