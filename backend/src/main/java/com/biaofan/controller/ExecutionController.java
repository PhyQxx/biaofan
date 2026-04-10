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

/**
 * SOP执行控制器
 * <p>管理SOP执行实例的生命周期，包括开始执行、完成步骤、激活、结束等操作。</p>
 *
 * @RestController
 * @RequestMapping("/api/execution")
 * @RequiredArgsConstructor
 */
@RestController
@RequestMapping("/api/execution")
@RequiredArgsConstructor
public class ExecutionController {

    private final SopExecutionService executionService;

    /**
     * 开始执行SOP
     * <p>用户选择某个SOP并开始执行，创建一个新的执行记录。</p>
     *
     * @param sopId  SOP ID
     * @param userId 当前登录用户ID（作为执行人）
     * @return 创建的执行记录
     */
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

    /**
     * 完成指定执行步骤
     * <p>用户完成当前步骤后调用，记录步骤完成情况，支持填写备注和检查数据。</p>
     *
     * @param executionId 执行记录ID
     * @param stepIndex  步骤索引（从0开始）
     * @param body       可选请求体，包含notes（备注）和checkData（检查数据）
     * @param userId     当前登录用户ID
     * @return 完成结果，包含是否全部完成及当前步骤号
     */
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

    /**
     * 激活执行记录
     * <p>将执行状态从暂停或待激活恢复为进行中。</p>
     *
     * @param executionId 执行记录ID
     * @param userId      当前登录用户ID
     * @return 操作结果
     */
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

    /**
     * 结束执行
     * <p>标记执行记录为已完成（completed）状态。</p>
     *
     * @param executionId 执行记录ID
     * @param userId      当前登录用户ID
     * @return 操作结果
     */
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

    /**
     * 获取当前用户的执行记录列表
     *
     * @param status 可选的执行状态过滤（pending/in_progress/completed/abnormal）
     * @param userId 当前登录用户ID
     * @return 执行记录列表
     */
    @GetMapping("/my")
    public Result<List<SopExecution>> myExecutions(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Long userId) {
        return Result.ok(executionService.getMyExecutions(userId, status));
    }

    /**
     * 获取指定执行记录详情
     * <p>仅允许执行人本人访问，会校验executorId与userId一致性。</p>
     *
     * @param executionId 执行记录ID
     * @param userId      当前登录用户ID
     * @return 执行记录详情
     */
    @GetMapping("/{executionId}")
    public Result<SopExecution> get(
            @PathVariable Long executionId,
            @AuthenticationPrincipal Long userId) {
        try {
            SopExecution e = executionService.getExecution(executionId);
            // H-04: 校验 executorId == userId，防止越权访问
            if (!e.getExecutorId().equals(userId)) {
                return Result.fail(403, "无权访问该执行记录");
            }
            return Result.ok(e);
        } catch (RuntimeException ex) {
            return Result.fail(404, ex.getMessage());
        }
    }

    /**
     * 获取执行记录关联的SOP详情
     * <p>返回该执行记录所对应的SOP模板信息（含步骤列表）。</p>
     *
     * @param executionId 执行记录ID
     * @param userId       当前登录用户ID（校验是否为执行人）
     * @return SOP详情
     */
    @GetMapping("/{executionId}/sop")
    public Result<Sop> getSop(
            @PathVariable Long executionId,
            @AuthenticationPrincipal Long userId) {
        try {
            SopExecution e = executionService.getExecution(executionId);
            // H-04: 校验 executorId == userId
            if (!e.getExecutorId().equals(userId)) {
                return Result.fail(403, "无权访问该执行记录");
            }
            Sop sop = executionService.getSopWithSteps(e.getSopId());
            return Result.ok(sop);
        } catch (RuntimeException ex) {
            return Result.fail(404, ex.getMessage());
        }
    }

    /**
     * 获取执行步骤记录列表
     * <p>返回该执行记录下所有步骤的完成情况与备注信息。</p>
     *
     * @param executionId 执行记录ID
     * @param userId       当前登录用户ID（校验是否为执行人）
     * @return 步骤记录列表
     */
    @GetMapping("/{executionId}/records")
    public Result<List<ExecutionStepRecord>> getRecords(
            @PathVariable Long executionId,
            @AuthenticationPrincipal Long userId) {
        try {
            SopExecution e = executionService.getExecution(executionId);
            // H-04: 校验 executorId == userId
            if (!e.getExecutorId().equals(userId)) {
                return Result.fail(403, "无权访问该执行记录");
            }
            return Result.ok(executionService.getStepRecords(executionId));
        } catch (RuntimeException ex) {
            return Result.fail(404, ex.getMessage());
        }
    }
}
