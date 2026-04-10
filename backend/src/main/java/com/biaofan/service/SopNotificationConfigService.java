package com.biaofan.service;

import com.biaofan.entity.SopNotificationConfig;

/**
 * SOP通知配置服务接口
 * 提供用户对各类通知渠道（飞书、邮箱等）的配置管理功能
 */
public interface SopNotificationConfigService {
    /**
     * 获取用户的通知配置
     * @param userId 用户ID
     * @param platform 平台类型（如feishu、email等）
     * @return 通知配置实体
     */
    SopNotificationConfig getConfig(Long userId, String platform);

    /**
     * 保存/更新通知配置
     * @param userId 用户ID
     * @param platform 平台类型
     * @param webhookUrl Webhook地址（飞书等场景）
     * @param secret 密钥
     * @param botName 机器人名称
     * @param triggerConfig 触发配置（JSON格式）
     * @param email 邮箱地址
     * @return 更新后的配置实体
     */
    SopNotificationConfig saveConfig(Long userId, String platform, String webhookUrl, String secret, String botName, String triggerConfig, String email);

    /**
     * 启用/禁用某个通知渠道
     * @param userId 用户ID
     * @param platform 平台类型
     * @param enabled 是否启用
     * @return 更新后的配置实体
     */
    SopNotificationConfig toggle(Long userId, String platform, Boolean enabled);
}
