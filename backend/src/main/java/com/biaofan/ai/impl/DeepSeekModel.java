package com.biaofan.ai.impl;

import com.biaofan.ai.AiModel;
import com.biaofan.entity.AiModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

/**
 * DeepSeek 模型实现
 * API 格式：OpenAI 兼容 https://api.deepseek.com/chat/completions
 */
@Component
public class DeepSeekModel implements AiModel {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekModel.class);

    @Override
    public String getType() {
        return "deepseek";
    }

    @Override
    public String chat(List<Map<String, String>> messages, AiModelConfig config) {
        String url = config.getApiUrl();
        if (url == null || url.isBlank()) {
            url = "https://api.deepseek.com/chat/completions";
        } else if (!url.contains("/chat/completions")) {
            url = url.replaceAll("/+$", "") + "/chat/completions";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName() != null ? config.getModelName() : "deepseek-chat");
        body.put("messages", messages);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModelName() != null ? config.getModelName() : "deepseek-chat");
        requestBody.put("messages", messages);
        if (config.getTemperature() != null) {
            requestBody.put("temperature", config.getTemperature());
        }

        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + config.getApiKey());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> resp = rt.postForEntity(url, entity, Map.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                List<?> choices = (List<?>) resp.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> msg = (Map<?, ?>) choice.get("message");
                    return msg != null ? (String) msg.get("content") : "";
                }
            }
        } catch (Exception e) {
            log.error("DeepSeek API 调用失败: {}", e.getMessage());
        }
        return "";
    }
}
