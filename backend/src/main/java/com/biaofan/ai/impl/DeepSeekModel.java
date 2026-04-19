package com.biaofan.ai.impl;

import com.biaofan.ai.AiModel;
import com.biaofan.ai.AiResult;
import com.biaofan.entity.AiModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * DeepSeek 模型实现
 * API 格式：OpenAI 兼容 https://api.deepseek.com/chat/completions
 */
@Component
public class DeepSeekModel implements AiModel {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekModel.class);

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String getType() {
        return "deepseek";
    }

    @Override
    public AiResult chat(List<Map<String, String>> messages, AiModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return AiResult.error("DeepSeek API Key 未配置");
        }

        String url = buildUrl(config.getApiUrl(), "https://api.deepseek.com/chat/completions", "/chat/completions");

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", config.getModelName() != null ? config.getModelName() : "deepseek-chat");
        requestBody.put("messages", messages);
        if (config.getTemperature() != null) {
            requestBody.put("temperature", config.getTemperature());
        }

        try {
            var headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + config.getApiKey());

            var entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
            var resp = restTemplate.postForEntity(url, entity, Map.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return parseResponse(resp.getBody());
            }
            return AiResult.error("DeepSeek 返回异常状态码: " + resp.getStatusCode());
        } catch (HttpClientErrorException e) {
            String detail = e.getResponseBodyAsString();
            log.error("DeepSeek API 客户端错误 {}: {}", e.getStatusCode(), detail);
            return AiResult.error("DeepSeek API 错误 " + e.getStatusCode() + ": " + detail);
        } catch (Exception e) {
            log.error("DeepSeek API 调用失败: {}", e.getMessage(), e);
            return AiResult.error("DeepSeek API 调用失败: " + e.getMessage());
        }
    }

    private AiResult parseResponse(Map<String, Object> body) {
        try {
            Object choicesObj = body.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                return AiResult.error("DeepSeek 响应格式异常: choices 为空");
            }
            Object choice = choices.get(0);
            if (!(choice instanceof Map<?, ?> choiceMap)) {
                return AiResult.error("DeepSeek 响应格式异常: choice 类型错误");
            }
            Object msgObj = choiceMap.get("message");
            if (!(msgObj instanceof Map<?, ?> msg)) {
                return AiResult.error("DeepSeek 响应格式异常: message 类型错误");
            }
            Object content = msg.get("content");
            if (content == null) {
                return AiResult.error("DeepSeek 响应 content 为空");
            }
            return AiResult.success((String) content);
        } catch (Exception e) {
            log.error("DeepSeek 响应解析失败: {}", e.getMessage());
            return AiResult.error("DeepSeek 响应解析失败: " + e.getMessage());
        }
    }

    /**
     * 智能构建 URL：自动补充 /chat/completions 后缀
     */
    String buildUrl(String configuredUrl, String defaultUrl, String suffix) {
        if (configuredUrl == null || configuredUrl.isBlank()) {
            return defaultUrl;
        }
        String trimmed = configuredUrl.replaceAll("/+$", "");
        if (!trimmed.contains("/chat/completions")) {
            return trimmed + suffix;
        }
        return trimmed;
    }
}
