package com.biaofan.dto.ai;

import lombok.Data;

/**
 * AI 对话请求
 */
@Data
public class AiChatRequest {
    /** 模型类型：deepseek / glm / minimax */
    private String modelType;
    /** 模型名称 */
    private String modelName;
    /** API 地址（可选） */
    private String apiUrl;
    /** API Key（可选） */
    private String apiKey;
    /** 系统提示词（可选） */
    private String systemPrompt;
    /** 消息内容 */
    private String content;
    /** 温度参数 */
    private Float temperature;
}
