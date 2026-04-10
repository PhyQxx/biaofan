package com.biaofan.controller;


/**
 * SOP 统计 Controller
 * - GET /api/sop/{sopId}/stats: 获取某 SOP 的执行统计数据
 *   - 执行次数、完成率、平均完成时长、异常率
 * - GET /api/sop/{sopId}/stats/export: 导出执行统计数据（Excel）
 */
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

/**
 * SOP统计控制器
 * <p>提供SOP执行的统计数据概览和Excel报表导出功能。
 * 包括执行次数、完成率、平均耗时、各SOP明细等。</p>
 *
 * @RestController
 * @RequestMapping("/api/sop/statistics")
 * @RequiredArgsConstructor
 */

/**
 * SOP 统计 Controller
 * - GET /api/sop/{sopId}/stats: 获取某 SOP 的执行统计数据
 *   - 执行次数、完成率、平均完成时长、异常率
 * - GET /api/sop/{sopId}/stats/export: 导出执行统计数据（Excel）
 */
@RestController
@RequestMapping("/api/sop/statistics")
@RequiredArgsConstructor
public class SopStatisticsController {

    private final SopMapper sopMapper;
    private final SopExecutionMapper executionMapper;
    private final ExecutionStatMapper statMapper;

    /**
     * 获取SOP执行统计概览
     * <p>返回总执行数、完成数、异常数、进行中数、完成率、平均耗时，以及各SOP的执行明细。</p>
     *
     * @param userId 当前登录用户ID
     * @param from   可选的开始日期过滤
     * @param to     可选的结束日期过滤
     * @return 统计结果
     */
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

    /**
     * 导出执行记录为Excel报表
     * <p>根据日期范围筛选执行记录，生成xlsx格式的Excel文件下载。</p>
     *
     * @param from     可选的开始日期
     * @param to       可选的结束日期
     * @param format   导出格式（默认xlsx）
     * @param response HTTP响应，用于文件下载
     * @throws IOException 文件写入异常
     */
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
