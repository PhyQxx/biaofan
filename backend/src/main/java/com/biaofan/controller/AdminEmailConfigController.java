package com.biaofan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.dto.Result;
import com.biaofan.entity.EmailConfig;
import com.biaofan.mapper.EmailConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Properties;

import java.time.LocalDateTime;

/**
 * 管理员邮件配置控制器
 * 管理全局邮件 SMTP 配置（单条记录 upsert）
 */
@RestController
@RequestMapping("/api/admin/email-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmailConfigController {

    private final EmailConfigMapper emailConfigMapper;

    /**
     * 获取邮件配置
     */
    @GetMapping
    public Result<EmailConfig> getConfig() {
        EmailConfig config = emailConfigMapper.selectOne(
            new LambdaQueryWrapper<EmailConfig>().last("LIMIT 1")
        );
        return Result.ok(config);
    }

    /**
     * 保存邮件配置（upsert）
     */
    @PostMapping
    public Result<EmailConfig> saveConfig(@RequestBody EmailConfig config) {
        EmailConfig existing = emailConfigMapper.selectOne(
            new LambdaQueryWrapper<EmailConfig>().last("LIMIT 1")
        );
        if (existing != null) {
            existing.setHost(config.getHost());
            existing.setPort(config.getPort());
            existing.setUsername(config.getUsername());
            if (config.getPassword() != null && !config.getPassword().isBlank()) {
                existing.setPassword(config.getPassword());
            }
            existing.setFromAddress(config.getFromAddress());
            existing.setSmtpAuth(config.getSmtpAuth());
            existing.setStarttlsEnable(config.getStarttlsEnable());
            existing.setEnabled(config.getEnabled());
            existing.setUpdatedAt(LocalDateTime.now());
            emailConfigMapper.updateById(existing);
            // 返回时隐藏密码
            existing.setPassword(null);
            return Result.ok(existing);
        } else {
            config.setId(null);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            if (config.getEnabled() == null) config.setEnabled(true);
            if (config.getSmtpAuth() == null) config.setSmtpAuth(true);
            if (config.getStarttlsEnable() == null) config.setStarttlsEnable(true);
            if (config.getFromAddress() == null || config.getFromAddress().isBlank()) {
                config.setFromAddress(config.getUsername());
            }
            emailConfigMapper.insert(config);
            // 返回时隐藏密码
            config.setPassword(null);
            return Result.ok(config);
        }
    }

    /**
     * 测试邮件发送
     * 使用当前保存的配置发送一封测试邮件到指定地址
     */
    @PostMapping("/test")
    public Result<String> testSend(@RequestBody TestRequest req) {
        EmailConfig config = emailConfigMapper.selectOne(
            new LambdaQueryWrapper<EmailConfig>().last("LIMIT 1")
        );
        if (config == null) {
            return Result.fail("请先保存邮件配置");
        }

        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(config.getHost());
            sender.setPort(config.getPort());
            sender.setUsername(config.getUsername());
            sender.setPassword(config.getPassword());

            Properties props = sender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", String.valueOf(Boolean.TRUE.equals(config.getSmtpAuth())));
            props.put("mail.smtp.starttls.enable", String.valueOf(Boolean.TRUE.equals(config.getStarttlsEnable())));
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.connectiontimeout", "5000");

            String fromAddress = config.getFromAddress();
            if (fromAddress == null || fromAddress.isBlank()) {
                fromAddress = config.getUsername();
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(req.getTo());
            message.setSubject("【标帆SOP】邮件配置测试");
            message.setText("这是一封测试邮件，用于验证邮件服务配置是否正确。\n\n发送时间：" + LocalDateTime.now());
            sender.send(message);

            return Result.ok("测试邮件已发送至 " + req.getTo());
        } catch (Exception e) {
            return Result.fail("发送失败：" + e.getMessage());
        }
    }

    public static class TestRequest {
        private String to;
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }
}
