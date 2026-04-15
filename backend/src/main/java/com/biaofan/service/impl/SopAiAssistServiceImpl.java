package com.biaofan.service.impl;

import com.biaofan.ai.AiModel;
import com.biaofan.ai.AiModelFactory;
import com.biaofan.entity.AiModelConfig;
import com.biaofan.entity.SopAiReview;
import com.biaofan.mapper.SopAiReviewMapper;
import com.biaofan.service.AiService;
import com.biaofan.service.SopAiAssistService;
import com.biaofan.dto.ai.SopAiAssistRequest;
import com.biaofan.dto.ai.SopAiCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SOP AI 辅助服务实现
 */
@Service
@RequiredArgsConstructor
public class SopAiAssistServiceImpl implements SopAiAssistService {

    private static final Logger log = LoggerFactory.getLogger(SopAiAssistServiceImpl.class);

    private final AiService aiService;
    private final AiModelFactory aiModelFactory;
    private final SopAiReviewMapper reviewMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== AI 创建 SOP ====================

    @Override
    public String generateSop(Long userId, SopAiCreateRequest request) {
        AiModelConfig config = aiService.getEffectiveConfig(userId);
        if (request.getModelType() != null) {
            config.setModelType(request.getModelType());
        }

        AiModel model = aiModelFactory.getModel(config.getModelType());
        String systemPrompt = config.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = model.buildCreateSystemPrompt();
        }

        String userPrompt = buildCreateUserPrompt(request);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        String response = aiModelFactory.chat(messages, config);
        return extractJson(response);
    }

    private String buildCreateUserPrompt(SopAiCreateRequest req) {
        StringBuilder sb = new StringBuilder("请帮我创建一个 SOP（标准操作程序）。\n\n");
        if (req.getGoal() != null && !req.getGoal().isBlank()) {
            sb.append("目标：").append(req.getGoal()).append("\n");
        }
        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            sb.append("标题：").append(req.getTitle()).append("\n");
        }
        if (req.getCategory() != null && !req.getCategory().isBlank()) {
            sb.append("分类：").append(req.getCategory()).append("\n");
        }
        if (req.getTags() != null && !req.getTags().isBlank()) {
            sb.append("标签：").append(req.getTags()).append("\n");
        }
        sb.append("\n请直接输出 JSON，不要额外解释。");
        return sb.toString();
    }

    // ==================== AI 执行指导 ====================

    @Override
    public String getExecuteGuidance(Long userId, SopAiAssistRequest request) {
        AiModelConfig config = aiService.getEffectiveConfig(userId);
        if (request.getModelType() != null) {
            config.setModelType(request.getModelType());
        }

        AiModel model = aiModelFactory.getModel(config.getModelType());
        String systemPrompt = model.buildExecuteSystemPrompt();

        String userPrompt = buildExecuteUserPrompt(request);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        return aiModelFactory.chat(messages, config);
    }

    private String buildExecuteUserPrompt(SopAiAssistRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("正在执行 SOP 步骤（第 ").append(req.getStepIndex() + 1)
                .append("/").append(req.getTotalSteps()).append(" 步）\n\n");
        if (req.getStepTitle() != null) {
            sb.append("步骤标题：").append(req.getStepTitle()).append("\n");
        }
        if (req.getStepDescription() != null) {
            sb.append("步骤说明：").append(req.getStepDescription()).append("\n");
        }
        if (req.getCheckData() != null && !req.getCheckData().isEmpty()) {
            sb.append("已填写的检查数据：").append(req.getCheckData().toString()).append("\n");
        }
        if (req.getNotes() != null && !req.getNotes().isBlank()) {
            sb.append("用户备注：").append(req.getNotes()).append("\n");
        }
        sb.append("\n请提供指导建议。");
        return sb.toString();
    }

    // ==================== AI 发布审核 ====================

    @Override
    public SopAiReview reviewSop(Long userId, Long sopId, String sopContentJson, Integer sopVersion) {
        AiModelConfig config = aiService.getEffectiveConfig(userId);

        AiModel model = aiModelFactory.getModel(config.getModelType());
        String systemPrompt = model.buildReviewSystemPrompt();

        String userPrompt = "请审核以下 SOP 内容：\n\n" + sopContentJson;

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        long start = System.currentTimeMillis();
        String response = aiModelFactory.chat(messages, config);
        long costMs = System.currentTimeMillis() - start;

        SopAiReview review = new SopAiReview();
        review.setSopId(sopId);
        review.setSopVersion(sopVersion);
        review.setReviewMode("review");
        review.setRawResponse(response);
        review.setModelType(config.getModelType());
        review.setCostMs(costMs);
        review.setCreatedAt(LocalDateTime.now());

        // 解析 JSON 响应
        parseAndSaveReview(response, review);
        reviewMapper.insert(review);
        return review;
    }

    private void parseAndSaveReview(String raw, SopAiReview review) {
        try {
            String json = extractJson(raw);
            Map<Object, Object> parsed = objectMapper.readValue(json, Map.class);
            review.setVerdict((String) parsed.getOrDefault("verdict", "warning"));

            Object issues = parsed.get("issues");
            if (issues != null) {
                review.setIssues(objectMapper.writeValueAsString(issues));
            }

            Object suggestions = parsed.get("suggestions");
            if (suggestions != null) {
                review.setSuggestions(objectMapper.writeValueAsString(suggestions));
            }
        } catch (Exception e) {
            log.warn("AI 审核响应 JSON 解析失败: {}", e.getMessage());
            review.setVerdict("warning");
        }
    }

    /**
     * 从 AI 返回内容中提取 JSON（处理 Markdown 代码块）
     */
    private String extractJson(String response) {
        if (response == null) return "";
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            int start = trimmed.indexOf("```json") + 7;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        } else if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("```") + 3;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }
        return trimmed;
    }
}
