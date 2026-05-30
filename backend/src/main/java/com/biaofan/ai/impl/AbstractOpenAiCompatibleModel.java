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

    /** 默认向量模型名 */
    protected abstract String defaultEmbeddingModelName();

    @Override
    public AiResult chat(List<Map<String, String>> messages, AiModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return AiResult.error(label() + " API Key 未配置");
        }

        String url = buildUrl(config.getApiUrl(), defaultApiUrl(), "/chat/completions");
        String modelName = config.getModelName() != null && !config.getModelName().isBlank() 
                ? config.getModelName() : defaultModelName();

        log.info("[AI Chat] Target URL: {}, Model: {}", url, modelName);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", messages);
        if (config.getTemperature() != null) {
            requestBody.put("temperature", config.getTemperature());
        }

        return executeRequest(url, requestBody, config.getApiKey(), this::parseChatResponse);
    }

    @Override
    public AiResult chatMultiModal(List<Map<String, Object>> messages, AiModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return AiResult.error(label() + " API Key 未配置");
        }

        String url = buildUrl(config.getApiUrl(), defaultApiUrl(), "/chat/completions");
        String modelName = config.getModelName() != null && !config.getModelName().isBlank() 
                ? config.getModelName() : defaultModelName();

        // 只有特定的多模态模型支持图片，通常需要用户在配置中指定模型名如 gpt-4o, claude-3-5-sonnet
        log.info("[AI MultiModal] Target URL: {}, Model: {}", url, modelName);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", messages);
        if (config.getTemperature() != null) {
            requestBody.put("temperature", config.getTemperature());
        }

        return executeRequest(url, requestBody, config.getApiKey(), this::parseChatResponse);
    }

    @Override
    public AiResult getEmbedding(String text, AiModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return AiResult.error(label() + " API Key 未配置");
        }

        String url = buildUrl(config.getApiUrl(), defaultApiUrl(), "/embeddings");
        String modelName = defaultEmbeddingModelName(); // 优先使用默认向量模型，后续可扩展至配置

        log.info("[AI Embedding] Target URL: {}, Model: {}", url, modelName);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("input", text);

        return executeRequest(url, requestBody, config.getApiKey(), this::parseEmbeddingResponse);
    }

    private AiResult executeRequest(String url, Object body, String apiKey, java.util.function.Function<Map<String, Object>, AiResult> parser) {
        try {
            var headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            var entity = new org.springframework.http.HttpEntity<>(body, headers);
            var resp = restTemplate.postForEntity(url, entity, Map.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return parser.apply(resp.getBody());
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

    private AiResult parseChatResponse(Map<String, Object> body) {
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

    private AiResult parseEmbeddingResponse(Map<String, Object> body) {
        try {
            Object dataObj = body.get("data");
            if (!(dataObj instanceof List<?> data) || data.isEmpty()) {
                return AiResult.error(label() + " 向量响应格式异常: data 为空");
            }
            Object item = data.get(0);
            if (!(item instanceof Map<?, ?> itemMap)) {
                return AiResult.error(label() + " 向量响应格式异常: item 类型错误");
            }
            Object embeddingObj = itemMap.get("embedding");
            if (!(embeddingObj instanceof List<?> embedding)) {
                return AiResult.error(label() + " 向量响应格式异常: embedding 类型错误");
            }
            return AiResult.success((List<Double>) embedding);
        } catch (Exception e) {
            log.error("{} 向量解析失败: {}", label(), e.getMessage());
            return AiResult.error(label() + " 向量解析失败: " + e.getMessage());
        }
    }

    String buildUrl(String configuredUrl, String defaultUrl, String suffix) {
        String base = (configuredUrl == null || configuredUrl.isBlank()) ? defaultUrl : configuredUrl;
        String trimmed = base.replaceAll("/+$", "");
        // 移除已有的后缀，确保可以灵活切换 /chat/completions 和 /embeddings
        trimmed = trimmed.replace("/chat/completions", "").replace("/embeddings", "");
        return trimmed + suffix;
    }
}
