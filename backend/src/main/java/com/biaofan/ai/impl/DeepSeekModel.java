package com.biaofan.ai.impl;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * DeepSeek 模型实现
 * API 格式：OpenAI 兼容 https://api.deepseek.com/chat/completions
 */
@Component
public class DeepSeekModel extends AbstractOpenAiCompatibleModel {

    public DeepSeekModel(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public String getType() {
        return "deepseek";
    }

    @Override
    protected String defaultApiUrl() {
        return "https://api.deepseek.com/chat/completions";
    }

    @Override
    protected String defaultModelName() {
        return "deepseek-chat";
    }

    @Override
    protected String urlSuffix() {
        return "/chat/completions";
    }

    @Override
    protected String label() {
        return "DeepSeek";
    }
}
