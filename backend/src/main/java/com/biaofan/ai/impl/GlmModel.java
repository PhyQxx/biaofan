package com.biaofan.ai.impl;

import com.biaofan.ai.AiModel;
import com.biaofan.entity.AiModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

/**
 * 智谱 GLM 模型实现
 * API 格式：https://open.bigmodel.cn/api/paas/v4/chat/completions
 */
@Component
public class GlmModel implements AiModel {

    private static final Logger log = LoggerFactory.getLogger(GlmModel.class);

    @Override
    public String getType() {
        return "glm";
    }

    @Override
    public String chat(List<Map<String, String>> messages, AiModelConfig config) {
        String url = config.getApiUrl();
        if (url == null || url.isBlank()) {
            url = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModelName() != null ? config.getModelName() : "glm-4-flash");
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
            log.error("GLM API 调用失败: {}", e.getMessage());
        }
        return "";
    }
}
