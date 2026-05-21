package com.biaofan.ai.impl;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * MiniMax 模型实现
 * 使用标准 OpenAI 兼容格式：/v1/chat/completions
 */
@Component
public class MiniMaxModel extends AbstractOpenAiCompatibleModel {

    public MiniMaxModel(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public String getType() {
        return "minimax";
    }

    @Override
    protected String defaultApiUrl() {
        return "https://api.minimax.chat/v1/chat/completions";
    }

    @Override
    protected String defaultModelName() {
        return "abab6.5s-chat";
    }

    @Override
    protected String urlSuffix() {
        return "/v1/chat/completions";
    }

    @Override
    protected String label() {
        return "MiniMax";
    }
}
