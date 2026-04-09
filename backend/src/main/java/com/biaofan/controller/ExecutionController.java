package com.biaofan.controller;

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

@RestController
@RequestMapping("/api/execution")
@RequiredArgsConstructor
public class ExecutionController {

    private final SopExecutionService executionService;

    @PostMapping("/start/{sopId}")
    public Result<SopExecution> start(
            @PathVariable Long sopId,
            @AuthenticationPrincipal Long userId) {
        try {
            SopExecution e = executionService.startExecution(userId, sopId);
            return Result.ok(e);
        } catch (RuntimeException ex) {
            return Result.fail(400, ex.getMessage());
        }
    }

    @PostMapping("/{executionId}/step/{stepIndex}")
    public Result<Map<String, Object>> completeStep(
            @PathVariable Long executionId,
            @PathVariable int stepIndex,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal Long userId) {
        try {
            String notes = body != null && body.get("notes") != null ? (String) body.get("notes") : "";
            @SuppressWarnings("unchecked")
            Map<String, Object> checkData = body != null && body.get("checkData") != null
                ? (Map<String, Object>) body.get("checkData") : null;
            boolean completed = executionService.completeStep(userId, executionId, stepIndex, notes, checkData);
            return Result.ok(Map.of("completed", completed, "currentStep", stepIndex >= executionService.getStepCount(executionId) ? stepIndex : stepIndex + 1));
        } catch (RuntimeException ex) {
            return Result.fail(400, ex.getMessage());
        }
    }

    @PostMapping("/{executionId}/activate")
    public Result<Void> activate(
            @PathVariable Long executionId,
            @AuthenticationPrincipal Long userId) {
        try {
            executionService.activateExecution(userId, executionId);
            return Result.ok();
        } catch (RuntimeException ex) {
            return Result.fail(400, ex.getMessage());
        }
    }

    @PostMapping("/{executionId}/finish")
    public Result<Void> finish(
            @PathVariable Long executionId,
            @AuthenticationPrincipal Long userId) {
        try {
            executionService.finishExecution(userId, executionId);
            return Result.ok();
        } catch (RuntimeException ex) {
            return Result.fail(400, ex.getMessage());
        }
    }

    @GetMapping("/my")
    public Result<List<SopExecution>> myExecutions(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Long userId) {
        return Result.ok(executionService.getMyExecutions(userId, status));
    }

    @GetMapping("/{executionId}")
    public Result<SopExecution> get(
            @PathVariable Long executionId,
            @AuthenticationPrincipal Long userId) {
        try {
            SopExecution e = executionService.getExecution(executionId);
            return Result.ok(e);
        } catch (RuntimeException ex) {
            return Result.fail(404, ex.getMessage());
        }
    }

    @GetMapping("/{executionId}/sop")
    public Result<Sop> getSop(
            @PathVariable Long executionId,
            @AuthenticationPrincipal Long userId) {
        try {
            SopExecution e = executionService.getExecution(executionId);
            Sop sop = executionService.getSopWithSteps(e.getSopId());
            return Result.ok(sop);
        } catch (RuntimeException ex) {
            return Result.fail(404, ex.getMessage());
        }
    }

    @GetMapping("/{executionId}/records")
    public Result<List<ExecutionStepRecord>> getRecords(
            @PathVariable Long executionId,
            @AuthenticationPrincipal Long userId) {
        try {
            executionService.getExecution(executionId); // verify access
            return Result.ok(executionService.getStepRecords(executionId));
        } catch (RuntimeException ex) {
            return Result.fail(404, ex.getMessage());
        }
    }
}
