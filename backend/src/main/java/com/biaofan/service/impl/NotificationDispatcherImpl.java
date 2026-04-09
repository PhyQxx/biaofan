package com.biaofan.service.impl;

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
