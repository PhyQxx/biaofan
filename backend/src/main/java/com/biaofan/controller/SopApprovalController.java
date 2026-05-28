package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.SopApprovalRecord;
import com.biaofan.mapper.SopApprovalRecordMapper;
import com.biaofan.service.SopService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sop")
@RequiredArgsConstructor
public class SopApprovalController {

    private final SopService sopService;
    private final SopApprovalRecordMapper approvalRecordMapper;

    /**
     * 提交审核
     */
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        try {
            sopService.submitForReview(id, userId);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 审核通过
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        try {
            sopService.approve(id, userId, body.get("comment"));
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(403, e.getMessage());
        }
    }

    /**
     * 审核驳回
     */
    @PostMapping("/{id}/reject")
    public Result<Void> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        try {
            sopService.reject(id, userId, body.get("comment"));
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(403, e.getMessage());
        }
    }

    /**
     * 获取待我审核的列表 (仅针对当前组织)
     */
    @GetMapping("/approvals/pending")
    public Result<List<SopApprovalRecord>> getPendingApprovals(
            @RequestParam Long orgId,
            @AuthenticationPrincipal Long userId) {
        // 简单查询该组织下所有待处理记录
        // TODO: 严格权限校验，确保只有管理员可调用
        return Result.ok(approvalRecordMapper.selectList(
            new LambdaQueryWrapper<SopApprovalRecord>()
                .eq(SopApprovalRecord::getOrgId, orgId)
                .eq(SopApprovalRecord::getStatus, "pending")
        ));
    }
}
