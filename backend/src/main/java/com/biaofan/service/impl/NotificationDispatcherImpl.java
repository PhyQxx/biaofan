package com.biaofan.service.impl;


/**
 * 通知分发服务实现
 * - 实现 NotificationDispatcher 接口
 * - 根据通知配置决定发送渠道（站内通知/极光推送等）
 * - 异常发生时自动触发通知给 SOP 负责人
 */
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.SopNotificationConfig;
import com.biaofan.mapper.SopNotificationConfigMapper;
import com.biaofan.service.NotificationDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * 通知分发器实现类
 * 异步将站内通知通过用户配置渠道发送（钉钉Webhook、企业邮件）
 * 根据平台类型构造不同格式的Webhook消息体
 *
 * @author biaofan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcherImpl implements NotificationDispatcher {

    private final SopNotificationConfigMapper configMapper;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 分发通知到第三方渠道
     * 根据用户配置发送Webhook和/或邮件
     * @param userId 用户ID
     * @param title 通知标题
     * @param content 通知内容
     */
    @Override
    @Async
    public void dispatch(Long userId, String title, String content) {
        SopNotificationConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SopNotificationConfig>()
                        .eq(SopNotificationConfig::getUserId, userId)
                        .last("LIMIT 1"));

        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }

        sendWebhook(config, title, content);
        sendEmail(config, title, content);
    }

    /**
     * 发送Webhook通知
     * 支持钉钉和企业微信等平台的消息格式
     */
    private void sendWebhook(SopNotificationConfig config, String title, String content) {
        if (config.getWebhookUrl() == null || config.getWebhookUrl().isBlank()) {
            return;
        }

        try {
            Map<String, Object> body;
            String platform = config.getPlatform();

            if ("dingtalk".equals(platform)) {
                body = Map.of(
                        "msgtype", "markdown",
                        "markdown", Map.of("title", title, "text", "## " + title + "\n\n" + content)
                );
            } else {
                body = Map.of(
                        "msg_type", "interactive",
                        "card", Map.of(
                                "header", Map.of("title", Map.of("content", title, "tag", "plain_text")),
                                "elements", java.util.List.of(Map.of("tag", "markdown", "content", content))
                        )
                );
            }

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getWebhookUrl()))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[Webhook] platform={} status={} body={}", platform, response.statusCode(), response.body());
        } catch (Exception e) {
            log.error("[Webhook] 发送失败 userId={} error={}", config.getUserId(), e.getMessage());
        }
    }

    /**
     * 发送邮件通知
     */
    private void sendEmail(SopNotificationConfig config, String title, String content) {
        if (config.getEmail() == null || config.getEmail().isBlank()) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("617594538@qq.com");
            message.setTo(config.getEmail());
            message.setSubject("【标帆SOP】" + title);
            message.setText(content);
            mailSender.send(message);
            log.info("[Email] 发送成功 to={}", config.getEmail());
        } catch (Exception e) {
            log.error("[Email] 发送失败 to={} error={}", config.getEmail(), e.getMessage());
        }
    }
}
