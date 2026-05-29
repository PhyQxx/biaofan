package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.ai.AiModel;
import com.biaofan.ai.AiModelFactory;
import com.biaofan.ai.AiResult;
import com.biaofan.entity.AiModelConfig;
import com.biaofan.mapper.AiModelConfigMapper;
import com.biaofan.service.AiService;
import com.biaofan.dto.ai.AiChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiModelConfigMapper configMapper;
    private final AiModelFactory aiModelFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final int MAX_AI_CALLS_PER_HOUR = 20;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    @Override
    @Cacheable(value = "aiConfig", key = "#userId")
    public AiModelConfig getEffectiveConfig(Long userId) {
        log.info("[AI Config] 开始获取生效配置 userId={}", userId);
        AiModelConfig userConfig = getUserConfig(userId);
        if (userConfig != null) {
            log.info("[AI Config] 使用用户级配置: id={}, type={}", userConfig.getId(), userConfig.getModelType());
            return userConfig;
        }

        // 查全局配置（user_id = NULL）
        AiModelConfig globalConfig = configMapper.selectOne(new LambdaQueryWrapper<AiModelConfig>()
                .isNull(AiModelConfig::getUserId)
                .eq(AiModelConfig::getEnabled, true)
                .last("LIMIT 1"));
        if (globalConfig == null) {
            log.error("[AI Config] 未发现全局配置");
            throw new RuntimeException("未配置全局 AI 模型，请前往管理员后台 → AI模型配置 进行设置");
        }
        log.info("[AI Config] 使用全局配置: id={}, type={}", globalConfig.getId(), globalConfig.getModelType());
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
    @CacheEvict(value = "aiConfig", key = "#userId")
    public void saveConfig(Long userId, AiModelConfig config) {
        AiModelConfig existing = getUserConfig(userId);
        if (existing != null) {
            existing.setModelType(config.getModelType());
            existing.setApiUrl(config.getApiUrl());
            // 只有当传入的 Key 不为空时才更新
            if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                existing.setApiKey(config.getApiKey());
            }
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
    @CacheEvict(value = "aiConfig", key = "#userId")
    public void deleteConfig(Long userId) {
        configMapper.delete(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getUserId, userId));
    }

    @Override
    @CacheEvict(value = "aiConfig", allEntries = true)
    @Transactional
    public void saveGlobalConfig(AiModelConfig config) {
        log.info("[AI Config] 保存全局配置: type={}, name={}", config.getModelType(), config.getModelName());
        AiModelConfig existing = configMapper.selectOne(
            new LambdaQueryWrapper<AiModelConfig>()
                .isNull(AiModelConfig::getUserId)
                .last("LIMIT 1")
        );
        if (existing != null) {
            existing.setModelType(config.getModelType());
            existing.setApiUrl(config.getApiUrl());
            // 只有当传入的 Key 不为空时才更新，防止 UI 编辑时误覆盖
            if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                existing.setApiKey(config.getApiKey());
            }
            existing.setModelName(config.getModelName());
            existing.setSystemPrompt(config.getSystemPrompt());
            existing.setTemperature(config.getTemperature());
            existing.setEnabled(config.getEnabled());
            existing.setUpdatedAt(LocalDateTime.now());
            configMapper.updateById(existing);
        } else {
            config.setUserId(null);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            if (config.getEnabled() == null) config.setEnabled(true);
            configMapper.insert(config);
        }
    }

    @Override
    @CacheEvict(value = "aiConfig", allEntries = true)
    public void deleteGlobalConfig(Long id) {
        log.info("[AI Config] 删除全局配置: id={}", id);
        AiModelConfig config = configMapper.selectById(id);
        if (config != null && config.getUserId() == null) {
            configMapper.deleteById(id);
        }
    }

    @Override
    public String chat(Long userId, AiChatRequest request) {
        // Rate limit check
        String key = "ai_rate:" + userId;
        Long calls = redisTemplate.opsForValue().increment(key);
        if (calls != null && calls == 1) {
            redisTemplate.expire(key, RATE_LIMIT_WINDOW);
        }
        if (calls != null && calls > MAX_AI_CALLS_PER_HOUR) {
            throw new RuntimeException("AI调用次数已达上限，请稍后再试");
        }

        AiModelConfig cached = getEffectiveConfig(userId);

        // 请求级覆盖
        AiModelConfig config = new AiModelConfig();
        config.setId(cached.getId());
        config.setUserId(cached.getUserId());
        config.setModelType(cached.getModelType());
        config.setApiUrl(cached.getApiUrl());
        config.setApiKey(cached.getApiKey());
        config.setModelName(cached.getModelName());
        config.setSystemPrompt(cached.getSystemPrompt());
        config.setEnabled(cached.getEnabled());
        config.setTemperature(cached.getTemperature());

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

        AiResult result = aiModelFactory.chat(messages, config);
        if (!result.isSuccess()) {
            log.warn("AI调用失败 userId={} error={}", userId, result.getError());
            return "AI服务暂时繁忙: " + result.getError();
        }
        return result.getContent();
    }
}
