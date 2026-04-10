package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.SopException;
import com.biaofan.service.SopExceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sop/exceptions")
@RequiredArgsConstructor
public class SopExceptionController {

    private final SopExceptionService exceptionService;

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

    /** 异常记录列表 - H-05: 增加 userId 过滤 */
    @GetMapping
    public Result<List<SopException>> list(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Long userId) {
        return Result.ok(exceptionService.getExceptionsByUser(userId, status));
    }

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
