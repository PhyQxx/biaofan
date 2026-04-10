package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.service.PushTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 推送服务控制器
 * 提供个推CID注册/更新功能，用于接收系统推送通知
 */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushTokenService pushTokenService;

    /**
     * 注册/更新推送Token
     * 将用户的个推ClientId与用户账号关联，用于接收推送通知
     * POST /api/push/register
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @param body 请求体，包含clientId（个推CID）和platform（平台：android/ios）
     * @return 操作结果
     */
    @PostMapping("/register")
    public Result<Void> register(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> body) {
        try {
            // C-04: userId 从 @AuthenticationPrincipal 获取，而非请求体
            String clientId = body.get("clientId").toString();
            String platform = body.get("platform") != null ? body.get("platform").toString() : "unknown";

            pushTokenService.registerToken(userId, clientId, platform);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(400, "注册失败: " + e.getMessage());
        }
    }
}
