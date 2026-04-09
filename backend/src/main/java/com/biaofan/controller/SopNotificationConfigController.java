package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.SopNotificationConfig;
import com.biaofan.service.SopNotificationConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-config")
@RequiredArgsConstructor
public class SopNotificationConfigController {

    private final SopNotificationConfigService configService;

    /** GET /api/notification-config?platform=feishu — 获取当前用户的通知配置 */
    @GetMapping
    public Result<SopNotificationConfig> getConfig(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "feishu") String platform) {
        SopNotificationConfig config = configService.getConfig(userId, platform);
        return Result.ok(config);
    }

    /**
     * POST /api/notification-config/save — 保存或更新通知配置
     * Body: { platform, webhookUrl, secret, botName, triggerConfig }
     */
    @PostMapping("/save")
    public Result<SopNotificationConfig> save(
            @AuthenticationPrincipal Long userId,
            @RequestBody SaveRequest req) {
        SopNotificationConfig config = configService.saveConfig(
            userId,
            req.getPlatform() != null ? req.getPlatform() : "feishu",
            req.getWebhookUrl(),
            req.getSecret(),
            req.getBotName(),
            req.getTriggerConfig(),
            req.getEmail()
        );
        return Result.ok(config);
    }

    /**
     * PUT /api/notification-config/toggle — 启用/禁用通知
     * Body: { platform, enabled }
     */
    @PutMapping("/toggle")
    public Result<SopNotificationConfig> toggle(
            @AuthenticationPrincipal Long userId,
            @RequestBody ToggleRequest req) {
        try {
            SopNotificationConfig config = configService.toggle(
                userId,
                req.getPlatform() != null ? req.getPlatform() : "feishu",
                req.getEnabled()
            );
            return Result.ok(config);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    public static class SaveRequest {
        private String platform;
        private String webhookUrl;
        private String secret;
        private String botName;
        private String triggerConfig;
        private String email;

        public String getPlatform() { return platform; }
        public String getWebhookUrl() { return webhookUrl; }
        public String getSecret() { return secret; }
        public String getBotName() { return botName; }
        public String getTriggerConfig() { return triggerConfig; }
        public String getEmail() { return email; }
    }

    public static class ToggleRequest {
        private String platform;
        private Boolean enabled;

        public String getPlatform() { return platform; }
        public Boolean getEnabled() { return enabled; }
    }
}
