package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.biaofan.entity.SopNotificationConfig;
import com.biaofan.mapper.SopNotificationConfigMapper;
import com.biaofan.service.SopNotificationConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SopNotificationConfigServiceImpl implements SopNotificationConfigService {

    private final SopNotificationConfigMapper mapper;

    @Override
    public SopNotificationConfig getConfig(Long userId, String platform) {
        return mapper.selectOne(new LambdaQueryWrapper<SopNotificationConfig>()
            .eq(SopNotificationConfig::getUserId, userId)
            .eq(SopNotificationConfig::getPlatform, platform)
            .last("LIMIT 1"));
    }

    @Override
    public SopNotificationConfig saveConfig(Long userId, String platform,
            String webhookUrl, String secret, String botName, String triggerConfig) {
        SopNotificationConfig existing = mapper.selectOne(new LambdaQueryWrapper<SopNotificationConfig>()
            .eq(SopNotificationConfig::getUserId, userId)
            .eq(SopNotificationConfig::getPlatform, platform)
            .last("LIMIT 1"));

        if (existing != null) {
            existing.setWebhookUrl(webhookUrl);
            existing.setSecret(secret);
            existing.setBotName(botName);
            existing.setTriggerConfig(triggerConfig);
            existing.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(existing);
            return existing;
        } else {
            SopNotificationConfig c = new SopNotificationConfig();
            c.setUserId(userId);
            c.setPlatform(platform);
            c.setWebhookUrl(webhookUrl);
            c.setSecret(secret);
            c.setBotName(botName);
            c.setEnabled(true);
            c.setTriggerConfig(triggerConfig);
            c.setCreatedAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            mapper.insert(c);
            return c;
        }
    }

    @Override
    public SopNotificationConfig toggle(Long userId, String platform, Boolean enabled) {
        SopNotificationConfig existing = mapper.selectOne(new LambdaQueryWrapper<SopNotificationConfig>()
            .eq(SopNotificationConfig::getUserId, userId)
            .eq(SopNotificationConfig::getPlatform, platform)
            .last("LIMIT 1"));

        if (existing == null) {
            throw new RuntimeException("通知配置不存在，请先保存配置");
        }
        existing.setEnabled(enabled);
        existing.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(existing);
        return existing;
    }
}
