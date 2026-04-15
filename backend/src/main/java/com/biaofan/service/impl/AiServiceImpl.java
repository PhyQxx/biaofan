package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.ai.AiModel;
import com.biaofan.ai.AiModelFactory;
import com.biaofan.entity.AiModelConfig;
import com.biaofan.mapper.AiModelConfigMapper;
import com.biaofan.service.AiService;
import com.biaofan.dto.ai.AiChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 服务实现
 */
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiModelConfigMapper configMapper;
    private final AiModelFactory aiModelFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiModelConfig getEffectiveConfig(Long userId) {
        AiModelConfig userConfig = getUserConfig(userId);
        if (userConfig != null) return userConfig;

        // 查全局配置（user_id = NULL）
        AiModelConfig globalConfig = configMapper.selectOne(new LambdaQueryWrapper<AiModelConfig>()
                .isNull(AiModelConfig::getUserId)
                .eq(AiModelConfig::getEnabled, true)
                .last("LIMIT 1"));
        if (globalConfig == null) {
            throw new RuntimeException("未配置全局 AI 模型，请前往管理员后台 → AI模型配置 进行设置");
        }
        return globalConfig;
    }

    @Override
    public AiModelConfig getUserConfig(Long userId) {
        return configMapper.selectOne(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getUserId, userId)
                .eq(AiModelConfig::getEnabled, true)
                .last("LIMIT 1"));
    }

    @Override
    public void saveConfig(Long userId, AiModelConfig config) {
        AiModelConfig existing = getUserConfig(userId);
        if (existing != null) {
            existing.setModelType(config.getModelType());
            existing.setApiUrl(config.getApiUrl());
            existing.setApiKey(config.getApiKey());
            existing.setModelName(config.getModelName());
            existing.setSystemPrompt(config.getSystemPrompt());
            existing.setTemperature(config.getTemperature());
            existing.setEnabled(config.getEnabled());
            existing.setUpdatedAt(LocalDateTime.now());
            configMapper.updateById(existing);
        } else {
            config.setUserId(userId);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            if (config.getEnabled() == null) config.setEnabled(true);
            configMapper.insert(config);
        }
    }

    @Override
    public void deleteConfig(Long userId) {
        configMapper.delete(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getUserId, userId));
    }

    @Override
    public String chat(Long userId, AiChatRequest request) {
        AiModelConfig config = getEffectiveConfig(userId);

        // 请求级覆盖
        if (request.getModelType() != null) config.setModelType(request.getModelType());
        if (request.getApiUrl() != null) config.setApiUrl(request.getApiUrl());
        if (request.getApiKey() != null) config.setApiKey(request.getApiKey());
        if (request.getModelName() != null) config.setModelName(request.getModelName());
        if (request.getTemperature() != null) config.setTemperature(request.getTemperature());

        List<Map<String, String>> messages = new ArrayList<>();

        String systemPrompt = request.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "你是一个专业的助手。";
        }
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", request.getContent()));

        return aiModelFactory.chat(messages, config);
    }
}
