package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.ExecutionStepRecord;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopExecution;
import com.biaofan.service.SopExecutionService;
import lombok.RequiredArgsConstructor;
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
    public Result<List<SopExecution>> myExecutions(
            @RequestParam(required = false) String executorId,
            @RequestParam(required = false) String status) {
        // executorId 参数由前端传入，实际使用在 service 层通过 @AuthenticationPrincipal 获取
        // 这里做路径转发，status 参数透传
        List<SopExecution> result = executionService.getMyExecutions(null, status);
        return Result.ok(result);
    }

    /**
     * POST /api/sop/executions/:sopId/start
     * 转发到: POST /api/execution/start/:sopId
     */
    @PostMapping("/{sopId}/start")
    public Result<SopExecution> start(
            @PathVariable Long sopId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            Long executorId = null;
            if (body != null && body.get("executorId") != null) {
                executorId = Long.valueOf(body.get("executorId").toString());
            }
            SopExecution e = executionService.startExecution(executorId, sopId);
            return Result.ok(e);
        } catch (RuntimeException ex) {
            return Result.fail(400, ex.getMessage());
        }
    }

    /**
     * POST /api/sop/executions/:execId/steps/:stepIndex/complete
     * 转发到: POST /api/execution/:execId/step/:stepIndex
     */
    @PostMapping("/{execId}/steps/{stepIndex}/complete")
    public Result<Map<String, Object>> completeStep(
            @PathVariable Long execId,
            @PathVariable int stepIndex,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String notes = body != null && body.get("notes") != null ? (String) body.get("notes") : "";
            @SuppressWarnings("unchecked")
            Map<String, Object> checkData = body != null && body.get("checkData") != null
                ? (Map<String, Object>) body.get("checkData") : null;
            String attachments = body != null && body.get("attachments") != null
                ? body.get("attachments").toString() : null;

            boolean completed = executionService.completeStep(null, execId, stepIndex, notes, checkData, attachments);
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
            @PathVariable Long execId) {
        try {
            return Result.ok(executionService.getStepRecords(execId));
        } catch (RuntimeException ex) {
            return Result.fail(404, ex.getMessage());
        }
    }
}
