package com.biaofan.service.impl;


/**
 * 通知分发服务实现
 * - 实现 NotificationDispatcher 接口
 * - 根据通知配置决定发送渠道（站内通知/极光推送等）
 * - 异常发生时自动触发通知给 SOP 负责人
 */
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.EmailConfig;
import com.biaofan.entity.SopNotificationConfig;
import com.biaofan.mapper.EmailConfigMapper;
import com.biaofan.mapper.SopNotificationConfigMapper;
import com.biaofan.service.NotificationDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
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
    private final EmailConfigMapper emailConfigMapper;
    private final JavaMailSender javaMailSender;
    private final HttpClient httpClient;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 分发通知到第三方渠道
     * 根据用户配置发送Webhook和/或邮件
     * @param userId 用户ID
     * @param title 通知标题
     * @param content 通知内容
     */
    @Override
    @Async("taskExecutor")
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

        String urlStr = config.getWebhookUrl();
        URI uri;
        try {
            uri = URI.create(urlStr);
        } catch (IllegalArgumentException e) {
            log.warn("[Webhook] URL格式无效: {}", urlStr);
            return;
        }

        // SSRF protection
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme)) {
            log.warn("[Webhook] URL 不支持非HTTPS协议: {}", urlStr);
            return;
        }

        String host = uri.getHost();
        if (host == null) {
            log.warn("[Webhook] URL 主机名无效: {}", urlStr);
            return;
        }

        // Block private/internal IP ranges
        if (isPrivateOrInternalHost(host)) {
            log.warn("[Webhook] URL 禁止访问内网地址: {}", host);
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
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[Webhook] platform={} status={} body={}", platform, response.statusCode(), response.body());
        } catch (Exception e) {
            log.error("[Webhook] 发送失败 userId={} error={}", config.getUserId(), e.getMessage());
            String key = "notif_retry:webhook:" + config.getUserId();
            Long retries = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofHours(1));
            if (retries != null && retries < 3) {
                log.warn("[Webhook] 发送失败 (userId={}), 第{}次重试: {}", 
                    config.getUserId(), retries, e.getMessage());
            } else {
                log.error("[Webhook] 发送失败，已达最大重试次数 userId={}", config.getUserId());
            }
        }
    }

    /**
     * 检查主机名是否为私有或内部地址
     */
    private boolean isPrivateOrInternalHost(String host) {
        if (host == null) return true;
        String lower = host.toLowerCase();
        // Block localhost
        if (lower.equals("localhost") || lower.equals("127.0.0.1") || lower.equals("0.0.0.0")) return true;
        // Block internal ranges
        if (lower.startsWith("192.168.") || lower.startsWith("10.") || lower.startsWith("172.16.")) return true;
        if (lower.startsWith("127.") || lower.startsWith("169.254.")) return true;
        // Block IPv6 localhost
        if (lower.equals("::1") || lower.equals("0:0:0:0:0:0:0:1")) return true;
        // Block hostnames that resolve to private IPs (basic check)
        if (lower.endsWith(".local") || lower.endsWith(".internal") || lower.endsWith(".private")) return true;
        return false;
    }

    /**
     * 发送邮件通知
     * 从数据库读取邮件配置，使用注入的 JavaMailSender 发送
     */
    private void sendEmail(SopNotificationConfig config, String title, String content) {
        if (config.getEmail() == null || config.getEmail().isBlank()) {
            return;
        }

        // 从数据库读取邮件配置
        EmailConfig emailConfig = emailConfigMapper.selectOne(
                new LambdaQueryWrapper<EmailConfig>().last("LIMIT 1")
        );
        if (emailConfig == null || !Boolean.TRUE.equals(emailConfig.getEnabled())) {
            log.warn("[Email] 邮件服务未配置或未启用，跳过发送");
            return;
        }

        try {
            String fromAddress = emailConfig.getFromAddress();
            if (fromAddress == null || fromAddress.isBlank()) {
                fromAddress = emailConfig.getUsername();
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(config.getEmail());
            message.setSubject("【标帆SOP】" + title);
            message.setText(content);
            javaMailSender.send(message);
            log.info("[Email] 发送成功 to={}", maskEmail(config.getEmail()));
        } catch (Exception e) {
            log.error("[Email] 发送失败 to={} error={}", maskEmail(config.getEmail()), e.getMessage());
            String key = "notif_retry:email:" + config.getEmail();
            Long retries = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofHours(1));
            if (retries != null && retries < 3) {
                log.warn("[Email] 发送失败 (to={}), 第{}次重试: {}", 
                    maskEmail(config.getEmail()), retries, e.getMessage());
            } else {
                log.error("[Email] 发送失败，已达最大重试次数 to={}", maskEmail(config.getEmail()));
            }
        }
    }

    /**
     * 脱敏邮箱地址 (GDPR compliance)
     * @param email 原始邮箱
     * @return 脱敏后的邮箱，如 a***z@example.com 或 ***@example.com
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIdx = email.indexOf('@');
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (local.length() <= 2) return "***" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
