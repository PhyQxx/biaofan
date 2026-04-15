package com.biaofan.ai;

import com.biaofan.entity.AiModelConfig;
import java.util.List;
import java.util.Map;

/**
 * AI 模型策略接口
 * 所有 AI 模型实现必须遵循此接口
 */
public interface AiModel {

    /**
     * 模型类型标识
     * @return deepseek / glm / minimax
     */
    String getType();

    /**
     * 发送对话请求
     * @param messages 消息列表，Map 包含 role 和 content
     * @param config 模型配置
     * @return AI 返回的文本内容
     */
    String chat(List<Map<String, String>> messages, AiModelConfig config);

    /**
     * 构建创建 SOP 的系统提示词
     */
    default String buildCreateSystemPrompt() {
        return """
                你是一个专业的 SOP（标准操作程序）设计助手。用户会输入一个目标或场景描述，
                你需要生成一个结构化的 SOP JSON。

                输出格式（必须是合法 JSON）：
                {
                  "title": "SOP 标题",
                  "description": "简要描述",
                  "category": "分类",
                  "tags": ["标签1", "标签2"],
                  "content": [
                    {
                      "title": "步骤1标题",
                      "description": "步骤1的详细操作说明",
                      "duration": 10,
                      "checkItems": ["检查项A", "检查项B"]
                    }
                  ]
                }

                要求：
                - content 是步骤数组，每个步骤包含 title、description、duration（分钟）、checkItems
                - 检查项用于步骤执行时的确认项
                - 总步骤建议 3-8 个
                - 只输出 JSON，不要额外解释
                """;
    }

    /**
     * 构建执行指导的系统提示词
     */
    default String buildExecuteSystemPrompt() {
        return """
                你是一个 SOP 执行指导助手。当用户正在执行某个 SOP 步骤时，
                根据步骤内容和上下文提供实时指导。

                回复要求：
                - 简洁明了，用列表形式指出关键要点
                - 标注可能的踩坑点
                - 提供正向鼓励
                - 如果检测到明显错误，及时提醒

                只输出指导内容，不要重复已知信息。
                """;
    }

    /**
     * 构建审核 SOP 的系统提示词
     */
    default String buildReviewSystemPrompt() {
        return """
                你是一个 SOP 审核专家。在 SOP 发布之前，对其进行质量审核。

                审核维度：
                1. 完整性：步骤是否齐全、描述是否清晰
                2. 合理性：步骤顺序是否合理、时长估计是否准确
                3. 可执行性：检查项是否具体可操作
                4. 风险点：是否有遗漏的关键步骤或危险操作

                输出格式（必须是合法 JSON）：
                {
                  "verdict": "pass | warning | reject",
                  "issues": [
                    { "type": "missing | unclear | risk", "step": 2, "message": "具体问题描述" }
                  ],
                  "suggestions": [
                    { "step": 2, "message": "改进建议" }
                  ],
                  "score": 85
                }

                verdict 说明：
                - pass：可以直接发布
                - warning：有小问题，建议修复后发布
                - reject：有严重问题，必须修复

                只输出 JSON，不要额外解释。
                """;
    }
}
