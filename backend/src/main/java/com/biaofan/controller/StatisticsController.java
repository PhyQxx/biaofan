package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.ExecutionStat;
import com.biaofan.service.ExecutionStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final ExecutionStatService statService;

    @GetMapping("/my")
    public Result<List<ExecutionStat>> myStats(@AuthenticationPrincipal Long userId) {
        return Result.ok(statService.getStats(userId));
    }

    @GetMapping("/global")
    public Result<List<ExecutionStat>> globalStats() {
        return Result.ok(statService.getGlobalStats());
    }
}
