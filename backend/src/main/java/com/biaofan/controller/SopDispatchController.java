package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.SopDispatch;
import com.biaofan.service.SopDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SOP分发控制器
 * <p>提供SOP模板批量分发给指定用户的功能，支持一次将多个模板分发给多个执行人。</p>
 *
 * @RestController
 * @RequestMapping("/api/sop")
 * @RequiredArgsConstructor
 */
@RestController
@RequestMapping("/api/sop")
@RequiredArgsConstructor
public class SopDispatchController {

    private final SopDispatchService dispatchService;

    /**
     * 批量分发SOP
     * <p>将指定的SOP模板批量分发给指定的用户列表，每个用户都会收到一份独立的执行实例。</p>
     *
     * @param userId       当前登录用户ID（分发操作人）
     * @param body         请求体，包含templateIds（模板ID列表）和assigneeIds（接收人ID列表）
     * @return 分发结果列表
     */
    /** 批量分发 SOP */
    @PostMapping("/batch-dispatch")
    public Result<List<SopDispatch>> batchDispatch(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, List<Integer>> body) {
        try {
            List<Integer> templateIdsInt = body.get("templateIds");
            List<Integer> assigneeIdsInt = body.get("assigneeIds");
            
            // Convert Integer to Long
            List<Long> templateIds = templateIdsInt.stream().map(Integer::longValue).collect(Collectors.toList());
            List<Long> assigneeIds = assigneeIdsInt.stream().map(Integer::longValue).collect(Collectors.toList());
            
            List<SopDispatch> results = dispatchService.batchDispatch(userId, templateIds, assigneeIds);
            return Result.ok(results);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 获取当前用户的分发记录列表
     * <p>返回当前用户发起的所有SOP分发记录。</p>
     *
     * @param userId 当前登录用户ID
     * @return 分发记录列表
     */
    /** 分发记录列表 */
    @GetMapping("/dispatches")
    public Result<List<SopDispatch>> dispatches(@AuthenticationPrincipal Long userId) {
        return Result.ok(dispatchService.getMyDispatches(userId));
    }
}
