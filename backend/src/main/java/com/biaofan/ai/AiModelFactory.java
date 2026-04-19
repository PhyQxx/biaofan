package com.biaofan.ai;

import com.biaofan.ai.impl.DeepSeekModel;
import com.biaofan.ai.impl.GlmModel;
import com.biaofan.ai.impl.MiniMaxModel;
import com.biaofan.entity.AiModelConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 模型工厂
 * 根据配置类型动态选择对应的 AI 模型实现
 */
@Component
@RequiredArgsConstructor
public class AiModelFactory {

    private final DeepSeekModel deepSeekModel;
    private final GlmModel glmModel;
    private final MiniMaxModel miniMaxModel;

    private static final Map<String, AiModel> MODEL_MAP = new ConcurrentHashMap<>();

    /**
     * 根据模型类型获取对应的 AI 模型实例
     */
    public AiModel getModel(String modelType) {
        return switch (modelType) {
            case "deepseek" -> deepSeekModel;
            case "glm" -> glmModel;
            case "minimax" -> miniMaxModel;
            default -> deepSeekModel;
        };
    }

    /**
     * 发送对话请求（快捷方法）
     */
    public AiResult chat(List<Map<String, String>> messages, AiModelConfig config) {
        AiModel model = getModel(config.getModelType());
        return model.chat(messages, config);
    }
}
