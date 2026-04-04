package com.biaofan.controller;

import com.biaofan.dto.Result;
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
    public Result<Void> completeStep(
            @PathVariable Long executionId,
            @PathVariable int stepIndex,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Long userId) {
        try {
            String notes = body != null ? body.get("notes") : "";
            executionService.completeStep(userId, executionId, stepIndex, notes);
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
}
