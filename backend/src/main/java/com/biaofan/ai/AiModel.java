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
     * @return AI 返回的结果，包含成功内容或错误信息
     */
    AiResult chat(List<Map<String, String>> messages, AiModelConfig config);

    /**
     * 获取文本的向量表示（Embeddings）
     * @param text 输入文本
     * @param config 模型配置
     * @return 包含 embedding 列表的结果
     */
    AiResult getEmbedding(String text, AiModelConfig config);

    /**
     * 发送多模态对话请求
     * @param messages 消息列表，content 可以是 String 或 List<Map<String, Object>>
     */
    AiResult chatMultiModal(List<Map<String, Object>> messages, AiModelConfig config);

    /**
     * 构建多模态验证的系统提示词
     */
    default String buildVerifySystemPrompt() {
        return """
                你是一个 SOP 执行验证专家。你将收到一个 SOP 步骤的描述以及用户上传的执行现场图片。
                你需要判断图片内容是否符合步骤的操作要求。

                输出格式（必须是合法 JSON）：
                {
                  "verdict": "pass | fail",
                  "reason": "判断依据的简要说明"
                }

                要求：
                - pass: 图片内容清晰且符合步骤要求。
                - fail: 图片内容不符、模糊或存在违规。
                - 只输出 JSON，不要额外解释。
                """;
    }

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

    /**
     * 构建预测后续步骤的系统提示词
     */
    default String buildPredictSystemPrompt() {
        return """
                你是一个 SOP 流程补全助手。根据当前的 SOP 标题和用户已经写好的步骤，
                预测接下来的 3 个逻辑步骤。

                输出格式（必须是合法 JSON 数组）：
                [
                  {
                    "title": "预测步骤标题",
                    "description": "预测步骤的详细操作说明",
                    "duration": 10,
                    "checkItems": ["检查项A"]
                  }
                ]

                要求：
                - 只输出 JSON 数组，包含且仅包含 3 个后续步骤
                - 步骤必须逻辑严密，承接已有内容
                - 描述要具体、标准
                - 不要额外解释
                """;
    }

    /**
     * 构建组织知识问答的系统提示词
     */
    default String buildOrgQaSystemPrompt() {
        return """
                你是一个专业的企业知识助手。你将获得该组织（公司）内部的一系列 SOP 标准操作程序文档。
                请根据这些文档内容，准确、专业地回答用户的提问。

                规则：
                1. 优先根据提供的 SOP 文档回答。
                2. 如果文档中没有相关信息，请明确说明，并基于通用专业知识给出建议，但需注明“非官方建议”。
                3. 语气要正式、专业。
                4. 如果涉及安全操作，务必强调规范。
                """;
    }

    /**
     * 构建 AI 异常诊断的系统提示词
     */
    default String buildDiagnosisSystemPrompt() {
        return """
                你是一个专业的 SOP 异常诊断专家。
                用户在执行某个 SOP 步骤时遇到了异常情况。你需要根据 SOP 定义、当前步骤背景和异常描述，
                分析可能的原因并提供改进建议或解决方法。

                回复要求：
                1. 诊断准确：结合步骤内容分析为什么会发生这个异常。
                2. 建议实操：给出具体的、可执行的纠正措施。
                3. 预防为主：提示如何避免下次发生同类问题。
                4. 语气专业、稳重。
                """;
    }

    /**
     * 构建文档/图片转 SOP 的系统提示词
     */
    default String buildParseDocumentSystemPrompt() {
        return """
                你是一个 SOP 建模专家。你将获得一份规章制度文档图片或流程图图片。
                你需要解析其中的逻辑，并将其转化为结构化的 SOP JSON。

                输出格式（必须是合法 JSON）：
                {
                  "title": "解析出的 SOP 标题",
                  "description": "流程概述",
                  "category": "分类",
                  "tags": ["标签1"],
                  "content": [
                    {
                      "title": "步骤1标题",
                      "description": "步骤1详细说明",
                      "duration": 10,
                      "checkItems": ["检查点"]
                    }
                  ]
                }

                要求：
                - 忠于原件逻辑，不要臆造步骤。
                - 如果是流程图，按路径节点生成步骤。
                - 只输出 JSON，不要额外解释。
                """;
    }
}
