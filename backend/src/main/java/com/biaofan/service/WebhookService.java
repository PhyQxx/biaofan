package com.biaofan.service;

/**
 * Webhook 通知服务
 * 支持向钉钉、飞书等外部平台推送通知
 */
public interface WebhookService {
    /**
     * 向组织配置的 Webhook 发送通知
     * @param orgId 组织ID
     * @param title 标题
     * @param content 内容
     */
    void sendNotification(Long orgId, String title, String content);
}
