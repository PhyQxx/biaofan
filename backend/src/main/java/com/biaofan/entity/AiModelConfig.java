package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 模型配置实体类
 * 支持多模型切换：DeepSeek / GLM(智谱) / MiniMax
 */
@Data
@TableName("ai_model_config")
public class AiModelConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID，NULL 表示全局配置 */
    private Long userId;
    /** 模型类型：deepseek / glm / minimax */
    private String modelType;
    /** API 地址 */
    private String apiUrl;
    /** API Key（加密存储） */
    private String apiKey;
    /** 模型名称，如 deepseek-chat / glm-4-flash / abab6.5s-chat */
    private String modelName;
    /** 系统提示词 */
    private String systemPrompt;
    /** 是否启用 */
    private Boolean enabled;
    /** 温度参数 */
    private Float temperature;
    /** MiniMax group_id（仅 minimax 模型需要） */
    private String groupId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
