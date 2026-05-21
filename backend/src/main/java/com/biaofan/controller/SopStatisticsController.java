package com.biaofan.controller;


/**
 * SOP 统计 Controller
 * - GET /api/sop/statistics: 获取执行统计概览（限制最多 10000 条，避免无界加载）
 * - GET /api/sop/statistics/export: 导出执行记录（Excel，分页批量加载，最多 50000 条）
 */
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.dto.Result;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopExecution;
import com.biaofan.mapper.SopExecutionMapper;
import com.biaofan.mapper.SopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SOP统计控制器
 * <p>提供SOP执行的统计数据概览和Excel报表导出功能。</p>
 */
@RestController
@RequestMapping("/api/sop/statistics")
@RequiredArgsConstructor
public class SopStatisticsController {

    private final SopMapper sopMapper;
    private final SopExecutionMapper executionMapper;

    /**
     * 获取SOP执行统计概览
     * <p>使用 LIMIT 10000 避免无界加载，只加载执行记录中涉及的 SOP。</p>
     */
    @GetMapping
    public Result<Map<String, Object>> statistics(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LambdaQueryWrapper<SopExecution> q = new LambdaQueryWrapper<>();
        if (from != null) q.ge(SopExecution::getCreatedAt, from.atStartOfDay());
        if (to != null) q.le(SopExecution::getCreatedAt, to.atTime(23, 59, 59));
        q.last("LIMIT 10000");

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

        // Per-SOP breakdown: only load SOPs that appear in the execution data
        Set<Long> sopIds = allExecs.stream().map(SopExecution::getSopId).collect(Collectors.toSet());
        Map<Long, Sop> sopMap = sopIds.isEmpty() ? Collections.emptyMap()
            : sopMapper.selectBatchIds(sopIds).stream().collect(Collectors.toMap(Sop::getId, s -> s));

        List<Map<String, Object>> sopStats = new ArrayList<>();
        Map<Long, List<SopExecution>> bySop = allExecs.stream().collect(Collectors.groupingBy(SopExecution::getSopId));
        for (var entry : bySop.entrySet()) {
            Long sopId = entry.getKey();
            List<SopExecution> sopExecs = entry.getValue();
            long sopTotal = sopExecs.size();
            long sopCompleted = sopExecs.stream().filter(e -> "completed".equals(e.getStatus())).count();
            Sop sop = sopMap.get(sopId);
            sopStats.add(Map.of(
                "sopId", (Object) sopId,
                "title", (Object) (sop != null ? sop.getTitle() : "未知"),
                "totalExecutions", (Object) sopTotal,
                "completedExecutions", (Object) sopCompleted,
                "completionRate", (Object) (sopTotal > 0 ? Math.round((double) sopCompleted / sopTotal * 10000.0) / 100.0 : 0.0)
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
     * <p>使用分页批量加载（每批 5000 条，最多 50000 条），避免一次性加载全部记录。</p>
     */
    @GetMapping("/export")
    public void export(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws IOException {

        SXSSFWorkbook workbook = new SXSSFWorkbook(1000);
        workbook.setCompressTempFiles(true);
        var sheet = workbook.createSheet("执行记录");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        var headerRow = sheet.createRow(0);
        String[] headers = {"执行ID", "SOP标题", "执行人ID", "状态", "当前步骤", "开始时间", "完成时间", "截止时间", "创建时间"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        int pageNum = 1;
        int batchSize = 5000;
        int maxRecords = 50000;
        int totalWritten = 1; // row 0 is header
        Map<Long, Sop> sopCache = new HashMap<>();

        while (totalWritten <= maxRecords) {
            LambdaQueryWrapper<SopExecution> q = new LambdaQueryWrapper<>();
            if (from != null) q.ge(SopExecution::getCreatedAt, from.atStartOfDay());
            if (to != null) q.le(SopExecution::getCreatedAt, to.atTime(23, 59, 59));
            q.orderByDesc(SopExecution::getCreatedAt);
            Page<SopExecution> p = new Page<>(pageNum, batchSize);
            Page<SopExecution> resultPage = executionMapper.selectPage(p, q);
            List<SopExecution> execs = resultPage.getRecords();

            if (execs.isEmpty()) break;

            // Batch load SOP titles for this batch
            Set<Long> uncachedIds = execs.stream().map(SopExecution::getSopId)
                .filter(id -> !sopCache.containsKey(id)).collect(Collectors.toSet());
            if (!uncachedIds.isEmpty()) {
                sopMapper.selectBatchIds(uncachedIds).forEach(s -> sopCache.put(s.getId(), s));
            }

            for (SopExecution exec : execs) {
                if (totalWritten > maxRecords) break;
                Sop sop = sopCache.get(exec.getSopId());
                var row = sheet.createRow(totalWritten++);
                row.createCell(0).setCellValue(exec.getId());
                row.createCell(1).setCellValue(sop != null ? sop.getTitle() : "未知");
                row.createCell(2).setCellValue(exec.getExecutorId());
                row.createCell(3).setCellValue(exec.getStatus() != null ? exec.getStatus() : "");
                row.createCell(4).setCellValue(exec.getCurrentStep() != null ? exec.getCurrentStep() : 0);
                row.createCell(5).setCellValue(exec.getStartedAt() != null ? exec.getStartedAt().format(fmt) : "");
                row.createCell(6).setCellValue(exec.getCompletedAt() != null ? exec.getCompletedAt().format(fmt) : "");
                row.createCell(7).setCellValue(exec.getDeadline() != null ? exec.getDeadline().format(fmt) : "");
                row.createCell(8).setCellValue(exec.getCreatedAt() != null ? exec.getCreatedAt().format(fmt) : "");
            }

            if (!resultPage.hasNext()) break;
            pageNum++;
        }

        String filename = "SOP执行报表_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition",
            "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + ".xlsx");

        OutputStream out = response.getOutputStream();
        workbook.write(out);
        workbook.dispose();
        workbook.close();
    }
}
