package com.biaofan.service;

import com.biaofan.entity.SopNotificationConfig;

public interface SopNotificationConfigService {
    SopNotificationConfig getConfig(Long userId, String platform);
    SopNotificationConfig saveConfig(Long userId, String platform, String webhookUrl, String secret, String botName, String triggerConfig, String email);
    SopNotificationConfig toggle(Long userId, String platform, Boolean enabled);
}
