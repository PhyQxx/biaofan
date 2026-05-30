package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.ai.AiModel;
import com.biaofan.ai.AiModelFactory;
import com.biaofan.ai.AiResult;
import com.biaofan.ai.VectorStore;
import com.biaofan.entity.AiModelConfig;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopAiReview;
import com.biaofan.mapper.SopAiReviewMapper;
import com.biaofan.mapper.SopMapper;
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
    private final VectorStore vectorStore;
    private final SopAiReviewMapper reviewMapper;
    private final SopMapper sopMapper;
    private final com.biaofan.mapper.SopExceptionMapper exceptionMapper;
    private final com.biaofan.mapper.SopExecutionMapper executionMapper;
    private final com.biaofan.mapper.ExecutionStepRecordMapper stepRecordMapper;
    private final ObjectMapper objectMapper;

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

        AiResult result = aiModelFactory.chat(messages, config);
        if (!result.isSuccess()) {
            throw new RuntimeException("AI 创建 SOP 失败: " + result.getError());
        }
        return extractJson(result.getContent());
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

        AiResult result = aiModelFactory.chat(messages, config);
        if (!result.isSuccess()) {
            throw new RuntimeException("AI 执行指导失败: " + result.getError());
        }
        return result.getContent();
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
        AiResult result = aiModelFactory.chat(messages, config);
        long costMs = System.currentTimeMillis() - start;

        SopAiReview review = new SopAiReview();
        review.setSopId(sopId);
        review.setSopVersion(sopVersion);
        review.setReviewMode("review");
        review.setModelType(config.getModelType());
        review.setCostMs(costMs);
        review.setCreatedAt(LocalDateTime.now());

        if (!result.isSuccess()) {
            log.error("AI 审核失败: {}", result.getError());
            review.setVerdict("warning");
            review.setRawResponse("AI 调用失败: " + result.getError());
        } else {
            review.setRawResponse(result.getContent());
            parseAndSaveReview(result.getContent(), review);
        }
        reviewMapper.insert(review);
        return review;
    }

    @Override
    public String predictNextSteps(Long userId, String title, String existingStepsJson) {
        AiModelConfig config = aiService.getEffectiveConfig(userId);
        AiModel model = aiModelFactory.getModel(config.getModelType());
        
        String systemPrompt = model.buildPredictSystemPrompt();
        String userPrompt = String.format("SOP 标题：%s\n当前已有步骤：\n%s\n\n请预测接下来的 3 个步骤。", 
                title, existingStepsJson);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        AiResult result = aiModelFactory.chat(messages, config);
        if (!result.isSuccess()) {
            throw new RuntimeException("AI 预测步骤失败: " + result.getError());
        }
        return extractJson(result.getContent());
    }

    @Override
    public String getOrgQa(Long userId, Long orgId, String question) {
        AiModelConfig config = aiService.getEffectiveConfig(userId);
        AiModel model = aiModelFactory.getModel(config.getModelType());
        
        // 1. 获取问题的向量
        AiResult embeddingResult = model.getEmbedding(question, config);
        if (!embeddingResult.isSuccess()) {
            log.error("Failed to generate embedding for QA: {}", embeddingResult.getError());
            return "AI 服务暂时繁忙，无法生成向量进行检索。";
        }

        // 2. 向量检索 top-k 相关的 SOP 片段
        List<VectorStore.SearchResult> results = vectorStore.search(embeddingResult.getEmbedding(), orgId, 5);
        
        if (results.isEmpty()) {
            return "抱歉，在您的组织知识库中未找到相关的 SOP 内容。";
        }

        // 3. 构造增强上下文
        StringBuilder context = new StringBuilder("以下是与提问相关的组织 SOP 知识片段：\n\n");
        for (VectorStore.SearchResult res : results) {
            context.append("--- 片段 (相关度: ").append(String.format("%.2f", res.getScore())).append(") ---\n");
            context.append(res.getContent()).append("\n\n");
        }

        String systemPrompt = model.buildOrgQaSystemPrompt();
        String userPrompt = context.toString() + "\n---\n用户提问：" + question;

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        AiResult result = aiModelFactory.chat(messages, config);
        if (!result.isSuccess()) {
            throw new RuntimeException("AI 知识问答失败: " + result.getError());
        }
        return result.getContent();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public String diagnoseException(Long userId, Long exceptionId) {
        com.biaofan.entity.SopException ex = exceptionMapper.selectById(exceptionId);
        if (ex == null) throw new RuntimeException("异常记录不存在");
        
        com.biaofan.entity.SopExecution exec = executionMapper.selectById(ex.getExecutionId());
        if (exec == null) throw new RuntimeException("执行记录不存在");
        
        com.biaofan.entity.Sop sop = sopMapper.selectById(exec.getSopId());
        if (sop == null) throw new RuntimeException("SOP不存在");

        AiModelConfig config = aiService.getEffectiveConfig(userId);
        AiModel model = aiModelFactory.getModel(config.getModelType());
        
        String systemPrompt = model.buildDiagnosisSystemPrompt();
        
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("SOP 标题：").append(sop.getTitle()).append("\n");
        userPrompt.append("异常步骤：第 ").append(exec.getCurrentStep()).append(" 步\n");
        userPrompt.append("异常描述：").append(ex.getDescription()).append("\n");
        userPrompt.append("异常类型：").append(ex.getType()).append("\n\n");
        
        userPrompt.append("请根据以上信息进行诊断并提供建议。");

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt.toString())
        );

        AiResult result = aiModelFactory.chat(messages, config);
        if (!result.isSuccess()) {
            throw new RuntimeException("AI 诊断失败: " + result.getError());
        }
        
        String diagnosis = result.getContent();
        ex.setAiDiagnosis(diagnosis);
        exceptionMapper.updateById(ex);
        
        return diagnosis;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Map<String, String> verifyStepImage(Long userId, Long recordId) {
        com.biaofan.entity.ExecutionStepRecord record = stepRecordMapper.selectById(recordId);
        if (record == null) throw new RuntimeException("步骤记录不存在");
        if (record.getImageUrl() == null || record.getImageUrl().isBlank()) {
            throw new RuntimeException("未上传图片，无法验证");
        }

        AiModelConfig config = aiService.getEffectiveConfig(userId);
        AiModel model = aiModelFactory.getModel(config.getModelType());

        String systemPrompt = model.buildVerifySystemPrompt();
        
        // 构建多模态内容 (遵循 OpenAI 格式)
        List<Map<String, Object>> userContent = new ArrayList<>();
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", "步骤标题：" + record.getStepTitle() + "\n请判断图片内容是否符合要求。");
        userContent.add(textPart);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        Map<String, String> imageUrlMap = new HashMap<>();
        imageUrlMap.put("url", record.getImageUrl());
        imagePart.put("image_url", imageUrlMap);
        userContent.add(imagePart);

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        );

        AiResult result = model.chatMultiModal(messages, config);
        if (!result.isSuccess()) {
            throw new RuntimeException("AI 验证失败: " + result.getError());
        }

        try {
            String json = extractJson(result.getContent());
            Map<String, String> parsed = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
            
            record.setVerificationResult(parsed.getOrDefault("verdict", "fail"));
            record.setVerificationReason(parsed.getOrDefault("reason", ""));
            stepRecordMapper.updateById(record);
            
            return parsed;
        } catch (Exception e) {
            log.error("Failed to parse AI verification response", e);
            throw new RuntimeException("AI 响应解析失败");
        }
    }

    @Override
    public String parseDocumentToSop(Long userId, String fileUrl) {
        AiModelConfig config = aiService.getEffectiveConfig(userId);
        AiModel model = aiModelFactory.getModel(config.getModelType());

        String systemPrompt = model.buildParseDocumentSystemPrompt();
        
        // 构建多模态内容
        List<Map<String, Object>> userContent = new ArrayList<>();
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", "请根据提供的图片/文档生成 SOP JSON。");
        userContent.add(textPart);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        Map<String, String> imageUrlMap = new HashMap<>();
        imageUrlMap.put("url", fileUrl);
        imagePart.put("image_url", imageUrlMap);
        userContent.add(imagePart);

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        );

        AiResult result = model.chatMultiModal(messages, config);
        if (!result.isSuccess()) {
            throw new RuntimeException("AI 解析文档失败: " + result.getError());
        }
        
        return extractJson(result.getContent());
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

    private String extractJson(String response) {
        if (response == null || response.isBlank()) return "{}";
        String trimmed = response.trim();
        int jsonStart = trimmed.indexOf("```json");
        if (jsonStart >= 0) {
            int codeStart = jsonStart + "```json".length();
            int codeEnd = trimmed.indexOf("```", codeStart);
            if (codeEnd > codeStart) return trimmed.substring(codeStart, codeEnd).trim();
        }
        int firstFence = trimmed.indexOf("```");
        if (firstFence >= 0) {
            int contentStart = firstFence + "```".length();
            int nextFence = trimmed.indexOf("```", contentStart);
            if (nextFence > contentStart) return trimmed.substring(contentStart, nextFence).trim();
        }
        return trimmed;
    }
}
