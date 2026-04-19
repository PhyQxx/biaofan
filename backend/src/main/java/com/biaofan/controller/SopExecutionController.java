package com.biaofan.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.dto.Result;
import com.biaofan.entity.ExecutionStepRecord;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopExecution;
import com.biaofan.service.SopExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SOP执行层 - 移动端路径别名控制器
 * 将 /api/sop/executions/* 映射到实际 /api/execution/* 路径
 */
@RestController
@RequestMapping("/api/sop/executions")
@RequiredArgsConstructor
public class SopExecutionController {

    private final SopExecutionService executionService;

    /**
     * GET /api/sop/executions/my?executorId=X
     * 转发到: GET /api/execution/my?executorId=X
     */
    @GetMapping("/my")
    public Result<Page<SopExecution>> myExecutions(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        Page<SopExecution> result = executionService.getMyExecutions(userId, status, page, pageSize);
        return Result.ok(result);
    }

    /**
     * POST /api/sop/executions/:sopId/start
     * 转发到: POST /api/execution/start/:sopId
     */
    @PostMapping("/{sopId}/start")
    public Result<SopExecution> start(
            @PathVariable Long sopId,
            @AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        try {
            SopExecution e = executionService.startExecution(userId, sopId);
            return Result.ok(e);
        } catch (RuntimeException ex) {
            return Result.fail(400, ex.getMessage());
        }
    }

    /**
     * POST /api/sop/executions/:execId/steps/:stepIndex/complete
     * 转发到: POST /api/execution/:execId/step/:stepIndex
     */
    @PostMapping("/{execId}/activate")
    public Result<Void> activate(
            @PathVariable Long execId,
            @AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        try {
            executionService.activateExecution(userId, execId);
            return Result.ok();
        } catch (RuntimeException ex) {
            return Result.fail(400, ex.getMessage());
        }
    }

    @PostMapping("/{execId}/steps/{stepIndex}/complete")
    public Result<Map<String, Object>> completeStep(
            @PathVariable Long execId,
            @PathVariable int stepIndex,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        try {
            String notes = body != null && body.get("notes") != null ? (String) body.get("notes") : "";
            @SuppressWarnings("unchecked")
            Map<String, Object> checkData = body != null && body.get("checkData") != null
                ? (Map<String, Object>) body.get("checkData") : null;
            boolean completed = executionService.completeStep(userId, execId, stepIndex, notes, checkData);
            return Result.ok(Map.of("completed", completed, "currentStep", stepIndex >= executionService.getStepCount(execId) ? stepIndex : stepIndex + 1));
        } catch (RuntimeException ex) {
            return Result.fail(400, ex.getMessage());
        }
    }

    /**
     * GET /api/sop/executions/:execId/record
     * 转发到: GET /api/execution/:execId/records
     */
    @GetMapping("/{execId}/record")
    public Result<List<ExecutionStepRecord>> getRecords(
            @PathVariable Long execId,
            @AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        try {
            executionService.getExecution(execId); // verify access
            return Result.ok(executionService.getStepRecords(execId));
        } catch (RuntimeException ex) {
            return Result.fail(404, ex.getMessage());
        }
    }
}
