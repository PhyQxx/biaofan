package com.biaofan.service.impl;

import com.biaofan.entity.Organization;
import com.biaofan.mapper.OrganizationMapper;
import com.biaofan.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final OrganizationMapper organizationMapper;
    private final RestTemplate restTemplate;

    @Override
    @Async
    public void sendNotification(Long orgId, String title, String content) {
        if (orgId == null) return;
        
        Organization org = organizationMapper.selectById(orgId);
        if (org == null || org.getWebhookUrl() == null || org.getWebhookUrl().isBlank()) {
            return;
        }

        log.info("[Webhook] Sending notification to org {}: {}", orgId, title);
        
        try {
            if ("dingtalk".equalsIgnoreCase(org.getWebhookType())) {
                sendToDingTalk(org.getWebhookUrl(), title, content);
            } else if ("feishu".equalsIgnoreCase(org.getWebhookType())) {
                sendToFeishu(org.getWebhookUrl(), title, content);
            }
        } catch (Exception e) {
            log.error("[Webhook] Failed to send notification to org {}: {}", orgId, e.getMessage());
        }
    }

    private void sendToDingTalk(String url, String title, String content) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("msgtype", "markdown");
        
        Map<String, String> markdown = new HashMap<>();
        markdown.put("title", title);
        markdown.put("text", "### " + title + "\n\n" + content);
        payload.put("markdown", markdown);

        restTemplate.postForEntity(url, payload, String.class);
    }

    private void sendToFeishu(String url, String title, String content) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("msg_type", "post");

        Map<String, Object> contentMap = new HashMap<>();
        Map<String, Object> post = new HashMap<>();
        Map<String, Object> zhCn = new HashMap<>();
        zhCn.put("title", title);
        
        java.util.List<java.util.List<Map<String, String>>> segments = new java.util.ArrayList<>();
        segments.add(java.util.List.of(Map.of("tag", "text", "text", content)));
        zhCn.put("content", segments);
        
        post.put("zh_cn", zhCn);
        contentMap.put("post", post);
        payload.put("content", contentMap);

        restTemplate.postForEntity(url, payload, String.class);
    }
}
