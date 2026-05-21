package com.biaofan.ai.impl;

import com.biaofan.ai.AiModel;
import com.biaofan.ai.AiResult;
import com.biaofan.entity.AiModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * OpenAI 兼容模型基类
 * 所有遵循 OpenAI chat/completions 格式的模型（DeepSeek、GLM、MiniMax 等）
 * 只需提供默认 URL、默认模型名和模型类型即可
 */
public abstract class AbstractOpenAiCompatibleModel implements AiModel {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RestTemplate restTemplate;

    protected AbstractOpenAiCompatibleModel(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** 默认 API URL（当用户未配置时使用） */
    protected abstract String defaultApiUrl();

    /** 默认模型名（当用户未配置时使用） */
    protected abstract String defaultModelName();

    /** URL 路径后缀，如 /chat/completions */
    protected abstract String urlSuffix();

    /** 模型标识前缀，用于日志（如 "DeepSeek"） */
    protected abstract String label();

    @Override
    public AiResult chat(List<Map<String, String>> messages, AiModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return AiResult.error(label() + " API Key 未配置");
        }

        String url = buildUrl(config.getApiUrl(), defaultApiUrl(), urlSuffix());

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", config.getModelName() != null ? config.getModelName() : defaultModelName());
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
            return AiResult.error(label() + " 返回异常状态码: " + resp.getStatusCode());
        } catch (HttpClientErrorException e) {
            String detail = e.getResponseBodyAsString();
            log.error("{} API 客户端错误 {}: {}", label(), e.getStatusCode(), detail);
            return AiResult.error(label() + " API 错误 " + e.getStatusCode() + ": " + detail);
        } catch (Exception e) {
            log.error("{} API 调用失败: {}", label(), e.getMessage(), e);
            return AiResult.error(label() + " API 调用失败: " + e.getMessage());
        }
    }

    private AiResult parseResponse(Map<String, Object> body) {
        try {
            Object choicesObj = body.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                return AiResult.error(label() + " 响应格式异常: choices 为空");
            }
            Object choice = choices.get(0);
            if (!(choice instanceof Map<?, ?> choiceMap)) {
                return AiResult.error(label() + " 响应格式异常: choice 类型错误");
            }
            Object msgObj = choiceMap.get("message");
            if (!(msgObj instanceof Map<?, ?> msg)) {
                return AiResult.error(label() + " 响应格式异常: message 类型错误");
            }
            Object content = msg.get("content");
            if (content == null) {
                return AiResult.error(label() + " 响应 content 为空");
            }
            return AiResult.success((String) content);
        } catch (Exception e) {
            log.error("{} 响应解析失败: {}", label(), e.getMessage());
            return AiResult.error(label() + " 响应解析失败: " + e.getMessage());
        }
    }

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
