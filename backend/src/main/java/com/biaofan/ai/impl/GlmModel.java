package com.biaofan.ai.impl;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 智谱 GLM 模型实现
 * API 格式：https://open.bigmodel.cn/api/paas/v4/chat/completions
 */
@Component
public class GlmModel extends AbstractOpenAiCompatibleModel {

    public GlmModel(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public String getType() {
        return "glm";
    }

    @Override
    protected String defaultApiUrl() {
        return "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    }

    @Override
    protected String defaultModelName() {
        return "glm-4-flash";
    }

    @Override
    protected String urlSuffix() {
        return "/chat/completions";
    }

    @Override
    protected String label() {
        return "GLM";
    }

    @Override
    protected String defaultEmbeddingModelName() {
        return "embedding-2";
    }
}
