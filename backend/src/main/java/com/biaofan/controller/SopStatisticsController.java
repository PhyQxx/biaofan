package com.biaofan.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.dto.Result;
import com.biaofan.entity.ExecutionStat;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopExecution;
import com.biaofan.mapper.ExecutionStatMapper;
import com.biaofan.mapper.SopExecutionMapper;
import com.biaofan.mapper.SopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sop/statistics")
@RequiredArgsConstructor
public class SopStatisticsController {

    private final SopMapper sopMapper;
    private final SopExecutionMapper executionMapper;
    private final ExecutionStatMapper statMapper;

    /** 统计概览 */
    @GetMapping
    public Result<Map<String, Object>> statistics(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LambdaQueryWrapper<SopExecution> q = new LambdaQueryWrapper<SopExecution>();
        if (from != null) q.ge(SopExecution::getCreatedAt, from.atStartOfDay());
        if (to != null) q.le(SopExecution::getCreatedAt, to.atTime(23, 59, 59));
        List<SopExecution> allExecs = executionMapper.selectList(q);

        long totalExecutions = allExecs.size();
        long completedExecutions = allExecs.stream().filter(e -> "completed".equals(e.getStatus())).count();
        long abnormalExecutions = allExecs.stream().filter(e -> "abnormal".equals(e.getStatus())).count();
        long pendingExecutions = allExecs.stream().filter(e -> "pending".equals(e.getStatus()) || "in_progress".equals(e.getStatus())).count();

        double avgDuration = 0;
        List<SopExecution> completed = allExecs.stream()
            .filter(e -> "completed".equals(e.getStatus()) && e.getCompletedAt() != null && e.getStartedAt() != null)
            .toList();
        if (!completed.isEmpty()) {
            long totalMinutes = completed.stream()
                .mapToLong(e -> java.time.Duration.between(e.getStartedAt(), e.getCompletedAt()).toMinutes())
                .sum();
            avgDuration = (double) totalMinutes / completed.size();
        }

        double completionRate = totalExecutions > 0 ? (double) completedExecutions / totalExecutions * 100 : 0;

        // Per-SOP breakdown
        List<Sop> sops = sopMapper.selectList(null);
        List<Map<String, Object>> sopStats = new ArrayList<>();
        for (Sop sop : sops) {
            List<SopExecution> sopExecs = allExecs.stream()
                .filter(e -> e.getSopId().equals(sop.getId()))
                .toList();
            long sopTotal = sopExecs.size();
            long sopCompleted = sopExecs.stream().filter(e -> "completed".equals(e.getStatus())).count();
            sopStats.add(Map.of(
                "sopId", sop.getId(),
                "title", sop.getTitle(),
                "totalExecutions", sopTotal,
                "completedExecutions", sopCompleted,
                "completionRate", sopTotal > 0 ? (double) sopCompleted / sopTotal * 100 : 0
            ));
        }

        return Result.ok(Map.of(
            "totalExecutions", totalExecutions,
            "completedExecutions", completedExecutions,
            "abnormalExecutions", abnormalExecutions,
            "pendingExecutions", pendingExecutions,
            "completionRate", Math.round(completionRate * 100.0) / 100.0,
            "avgDurationMinutes", Math.round(avgDuration * 100.0) / 100.0,
            "sopStats", sopStats
        ));
    }

    /** 导出 Excel */
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "xlsx") String format,
            HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<SopExecution> q = new LambdaQueryWrapper<SopExecution>();
        if (from != null) q.ge(SopExecution::getCreatedAt, from.atStartOfDay());
        if (to != null) q.le(SopExecution::getCreatedAt, to.atTime(23, 59, 59));
        q.orderByDesc(SopExecution::getCreatedAt);
        List<SopExecution> execs = executionMapper.selectList(q);

        List<Map<String, Object>> rows = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (SopExecution exec : execs) {
            Sop sop = sopMapper.selectById(exec.getSopId());
            rows.add(Map.of(
                "执行ID", exec.getId(),
                "SOP标题", sop != null ? sop.getTitle() : "未知",
                "执行人ID", exec.getExecutorId(),
                "状态", exec.getStatus(),
                "当前步骤", exec.getCurrentStep(),
                "开始时间", exec.getStartedAt() != null ? exec.getStartedAt().format(fmt) : "",
                "完成时间", exec.getCompletedAt() != null ? exec.getCompletedAt().format(fmt) : "",
                "截止时间", exec.getDeadline() != null ? exec.getDeadline().format(fmt) : "",
                "创建时间", exec.getCreatedAt() != null ? exec.getCreatedAt().format(fmt) : ""
            ));
        }

        String filename = "SOP执行报表_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition",
            "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + ".xlsx");

        EasyExcel.write(response.getOutputStream())
            .sheet("执行记录")
            .doWrite(rows);
    }
}
