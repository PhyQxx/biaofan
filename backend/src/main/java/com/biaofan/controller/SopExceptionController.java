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

    /** 异常记录列表 */
    @GetMapping
    public Result<List<SopException>> list(@RequestParam(required = false) String status) {
        return Result.ok(exceptionService.getExceptions(status));
    }

    /** 标记异常已处理 */
    @PutMapping("/{id}/resolve")
    public Result<Void> resolve(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        try {
            String resolution = body.get("resolution");
            exceptionService.resolve(id, userId, resolution);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
