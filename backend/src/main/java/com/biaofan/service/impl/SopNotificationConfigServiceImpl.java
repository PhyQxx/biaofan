package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.biaofan.entity.SopNotificationConfig;
import com.biaofan.mapper.SopNotificationConfigMapper;
import com.biaofan.service.SopNotificationConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * SOP通知配置服务实现类
 * 管理用户的消息通知推送配置，包括钉钉Webhook、企业邮箱等渠道
 * 支持配置的查询、保存和启用/禁用切换
 *
 * @author biaofan
 */
@Service
@RequiredArgsConstructor
public class SopNotificationConfigServiceImpl implements SopNotificationConfigService {

    private final SopNotificationConfigMapper mapper;

    /**
     * 获取用户的通知配置
     * @param userId 用户ID
     * @param platform 平台类型（如 dingtalk、feishu）
     * @return 通知配置实体
     */
    @Override
    public SopNotificationConfig getConfig(Long userId, String platform) {
        return mapper.selectOne(new LambdaQueryWrapper<SopNotificationConfig>()
            .eq(SopNotificationConfig::getUserId, userId)
            .eq(SopNotificationConfig::getPlatform, platform)
            .last("LIMIT 1"));
    }

    /**
     * 保存或更新通知配置
     * @param userId 用户ID
     * @param platform 平台类型
     * @param webhookUrl Webhook地址
     * @param secret 密钥
     * @param botName 机器人名称
     * @param triggerConfig 触发配置（JSON）
     * @param email 邮箱地址
     * @return 保存后的配置实体
     */
    @Override
    public SopNotificationConfig saveConfig(Long userId, String platform,
            String webhookUrl, String secret, String botName, String triggerConfig, String email) {
        SopNotificationConfig existing = mapper.selectOne(new LambdaQueryWrapper<SopNotificationConfig>()
            .eq(SopNotificationConfig::getUserId, userId)
            .eq(SopNotificationConfig::getPlatform, platform)
            .last("LIMIT 1"));

        if (existing != null) {
            existing.setWebhookUrl(webhookUrl);
            existing.setSecret(secret);
            existing.setBotName(botName);
            existing.setTriggerConfig(triggerConfig);
            if (email != null) existing.setEmail(email);
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
            c.setEmail(email);
            c.setCreatedAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            mapper.insert(c);
            return c;
        }
    }

    /**
     * 启用或禁用通知配置
     * @param userId 用户ID
     * @param platform 平台类型
     * @param enabled 是否启用
     * @return 更新后的配置实体
     */
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
