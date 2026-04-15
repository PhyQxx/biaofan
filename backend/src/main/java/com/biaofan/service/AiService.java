package com.biaofan.service;

import com.biaofan.dto.ai.AiChatRequest;
import com.biaofan.entity.AiModelConfig;
import java.util.List;

/**
 * AI 模型配置服务接口
 */
public interface AiService {

    /**
     * 获取用户的 AI 模型配置（优先用户级，否则全局）
     */
    AiModelConfig getEffectiveConfig(Long userId);

    /**
     * 获取用户配置（可为空）
     */
    AiModelConfig getUserConfig(Long userId);

    /**
     * 保存/更新用户 AI 配置
     */
    void saveConfig(Long userId, AiModelConfig config);

    /**
     * 删除用户 AI 配置
     */
    void deleteConfig(Long userId);

    /**
     * 通用对话接口
     */
    String chat(Long userId, AiChatRequest request);
}
