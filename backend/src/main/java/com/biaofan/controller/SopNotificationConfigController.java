package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.SopNotificationConfig;
import com.biaofan.service.SopNotificationConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * SOP通知配置控制器
 * <p>提供通知渠道（飞书、邮件等）的配置管理，包括获取、保存、启用/禁用等操作。</p>
 *
 * @RestController
 * @RequestMapping("/api/notification-config")
 * @RequiredArgsConstructor
 */
@RestController
@RequestMapping("/api/notification-config")
@RequiredArgsConstructor
public class SopNotificationConfigController {

    private final SopNotificationConfigService configService;

    /**
     * 获取当前用户的通知配置
     *
     * @param userId   当前登录用户ID
     * @param platform 通知平台（默认feishu）
     * @return 通知配置
     */
    /** GET /api/notification-config?platform=feishu — 获取当前用户的通知配置 */
    @GetMapping
    public Result<SopNotificationConfig> getConfig(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "feishu") String platform) {
        SopNotificationConfig config = configService.getConfig(userId, platform);
        return Result.ok(config);
    }

    /**
     * 保存或更新通知配置
     *
     * @param userId 当前登录用户ID
     * @param req    保存请求体，包含platform、webhookUrl、secret、botName、triggerConfig、email
     * @return 保存后的通知配置
     */
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
     * 启用或禁用通知
     *
     * @param userId 当前登录用户ID
     * @param req   切换请求体，包含platform和enabled
     * @return 更新后的通知配置
     */
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
