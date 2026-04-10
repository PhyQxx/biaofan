package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.SopException;
import com.biaofan.service.SopExceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SOP异常控制器
 * <p>提供SOP执行过程中异常的上报、查询、标记处理等功能。</p>
 *
 * @RestController
 * @RequestMapping("/api/sop/exceptions")
 * @RequiredArgsConstructor
 */
@RestController
@RequestMapping("/api/sop/exceptions")
@RequiredArgsConstructor
public class SopExceptionController {

    private final SopExceptionService exceptionService;

    /**
     * 上报执行异常
     * <p>用户在执行SOP过程中遇到问题时调用，记录异常类型和描述。</p>
     *
     * @param executionId 执行记录ID
     * @param userId      当前登录用户ID
     * @param body        请求体，包含type（异常类型）和description（描述）
     * @return 创建的异常记录
     */
    /** 上报执行异常 */
    @PostMapping("/{executionId}/report")
    public Result<SopException> report(
            @PathVariable Long executionId,
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        try {
            String type = body.get("type");
            String description = body.get("description");
            SopException ex = exceptionService.reportException(userId, executionId, type, description);
            return Result.ok(ex);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 获取当前用户的异常记录列表
     * <p>可按状态（pending/resolved）过滤，返回当前用户上报的异常。</p>
     *
     * @param status 可选的异常状态过滤
     * @param userId 当前登录用户ID
     * @return 异常记录列表
     */
    /** 异常记录列表 - H-05: 增加 userId 过滤 */
    @GetMapping
    public Result<List<SopException>> list(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Long userId) {
        return Result.ok(exceptionService.getExceptionsByUser(userId, status));
    }

    /**
     * 标记异常已处理
     * <p>管理员将异常标记为已处理，需填写处理说明。仅管理员可操作。</p>
     *
     * @param id     异常记录ID
     * @param userId 当前登录用户ID（应为管理员）
     * @param body   请求体，包含resolution（处理说明）
     * @return 操作结果
     */
    /** 标记异常已处理 - H-06: 仅管理员可操作 */
    @PutMapping("/{id}/resolve")
    public Result<Void> resolve(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        try {
            // H-06: 管理员权限校验由 SecurityConfig 中 /api/admin/** 的 hasRole("ADMIN") 保证
            // 此接口移至 /api/admin/ 路径下，此处保留但加权限提示
            String resolution = body.get("resolution");
            exceptionService.resolve(id, userId, resolution);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
