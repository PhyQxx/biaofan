package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.service.PushTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushTokenService pushTokenService;

    /**
     * POST /api/push/register
     * 个推CID注册/更新
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            String clientId = body.get("clientId").toString();
            String platform = body.get("platform").toString();

            pushTokenService.registerToken(userId, clientId, platform);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(400, "注册失败: " + e.getMessage());
        }
    }
}
