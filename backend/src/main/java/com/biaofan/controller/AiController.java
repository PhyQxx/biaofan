package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopAiReview;
import com.biaofan.mapper.SopMapper;
import com.biaofan.service.SopAiAssistService;
import com.biaofan.dto.ai.AiChatRequest;
import com.biaofan.dto.ai.SopAiAssistRequest;
import com.biaofan.dto.ai.SopAiCreateRequest;
import com.biaofan.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * SOP AI 辅助接口
 * 三大功能：AI 创建 SOP / 执行指导 / 发布审核
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final SopAiAssistService sopAiAssistService;
    private final AiService aiService;
    private final SopMapper sopMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String AI_RATE_PREFIX = "ai_rate:";
    private static final int AI_RATE_LIMIT = 10; // 每用户每分钟 10 次
    private static final Duration AI_RATE_WINDOW = Duration.ofMinutes(1);

    /**
     * AI 接口限流检查，超限返回 true
     */
    private boolean isAiRateLimited(Long userId) {
        String key = AI_RATE_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, AI_RATE_WINDOW);
        }
        return count != null && count > AI_RATE_LIMIT;
    }

    /**
     * AI 辅助创建 SOP
     * POST /api/ai/sop/generate
     *
     * Body: { goal, title?, category?, tags?, modelType? }
     * Return: { title, description, category, tags, content: [...] }
     */
    @PostMapping("/sop/generate")
    public Result<String> generateSop(@AuthenticationPrincipal Long userId,
                                     @Valid @RequestBody SopAiCreateRequest request) {
        if (isAiRateLimited(userId)) {
            return Result.fail(429, "AI 请求过于频繁，请稍后再试");
        }
        String sopJson = sopAiAssistService.generateSop(userId, request);
        return Result.ok(sopJson);
    }

    /**
     * AI 执行指导
     * POST /api/ai/sop/execute/guidance
     *
     * Body: { stepTitle, stepDescription, stepIndex, totalSteps, checkData?, notes?, modelType? }
     * Return: AI 指导文本
     */
    @PostMapping("/sop/execute/guidance")
    public Result<String> getExecuteGuidance(@AuthenticationPrincipal Long userId,
                                              @Valid @RequestBody SopAiAssistRequest request) {
        if (isAiRateLimited(userId)) {
            return Result.fail(429, "AI 请求过于频繁，请稍后再试");
        }
        String guidance = sopAiAssistService.getExecuteGuidance(userId, request);
        return Result.ok(guidance);
    }

    /**
     * AI 发布审核
     * POST /api/ai/sop/review
     *
     * Body: { sopId }
     * Return: { verdict, issues, suggestions, score, rawResponse }
     */
    @PostMapping("/sop/review")
    public Result<SopAiReview> reviewSop(@AuthenticationPrincipal Long userId,
                                         @Valid @RequestBody SopAiReviewRequest request) {
        Sop sop = sopMapper.selectById(request.getSopId());
        if (sop == null) {
            return Result.fail(404, "SOP 不存在");
        }
        if (!sop.getUserId().equals(userId)) {
            return Result.fail(403, "无权操作");
        }
        SopAiReview review = sopAiAssistService.reviewSop(
                userId, sop.getId(), sop.getContent(), sop.getVersion());
        return Result.ok(review);
    }

    /**
     * AI 步骤智能补全
     * POST /api/ai/sop/predict-next
     *
     * Body: { title, existingStepsJson }
     */
    @PostMapping("/sop/predict-next")
    public Result<String> predictNextSteps(@AuthenticationPrincipal Long userId,
                                            @Valid @RequestBody PredictNextRequest request) {
        if (isAiRateLimited(userId)) {
            return Result.fail(429, "AI 请求过于频繁，请稍后再试");
        }
        String stepsJson = sopAiAssistService.predictNextSteps(userId, request.getTitle(), request.getExistingStepsJson());
        return Result.ok(stepsJson);
    }

    /**
     * 组织级智能 Q&A
     * POST /api/ai/org/qa
     */
    @PostMapping("/org/qa")
    public Result<String> getOrgQa(@AuthenticationPrincipal Long userId,
                                   @Valid @RequestBody OrgQaRequest request) {
        if (isAiRateLimited(userId)) {
            return Result.fail(429, "AI 请求过于频繁，请稍后再试");
        }
        if (request.getOrgId() == null) {
            return Result.fail(400, "orgId 不能为空");
        }
        String reply = sopAiAssistService.getOrgQa(userId, request.getOrgId(), request.getQuestion());
        return Result.ok(reply);
    }

    /**
     * 通用 AI 对话（自由对话）
     * POST /api/ai/chat
     */
    @PostMapping("/chat")
    public Result<String> chat(@AuthenticationPrincipal Long userId,
                               @Valid @RequestBody AiChatRequest request) {
        if (isAiRateLimited(userId)) {
            return Result.fail(429, "AI 请求过于频繁，请稍后再试");
        }
        String reply = aiService.chat(userId, request);
        return Result.ok(reply);
    }

    // ==================== 请求 DTO ====================

    @lombok.Data
    public static class SopAiReviewRequest {
        private Long sopId;
    }

    @lombok.Data
    public static class PredictNextRequest {
        private String title;
        private String existingStepsJson;
    }

    @lombok.Data
    public static class OrgQaRequest {
        private Long orgId;
        private String question;
    }
}
