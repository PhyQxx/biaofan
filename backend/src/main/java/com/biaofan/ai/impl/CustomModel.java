package com.biaofan.ai.impl;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 自定义 OpenAI 兼容模型
 * 允许用户通过配置 API 地址和模型名称来使用任何兼容 OpenAI 协议的服务
 */
@Component
public class CustomModel extends AbstractOpenAiCompatibleModel {

    public CustomModel(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public String getType() {
        return "custom";
    }

    @Override
    protected String defaultApiUrl() {
        return ""; // 自定义类型必须由用户在配置中提供 API 地址
    }

    @Override
    protected String defaultModelName() {
        return ""; // 自定义类型必须由用户在配置中提供模型名称
    }

    @Override
    protected String urlSuffix() {
        return "/chat/completions";
    }

    @Override
    protected String label() {
        return "Custom";
    }
}
