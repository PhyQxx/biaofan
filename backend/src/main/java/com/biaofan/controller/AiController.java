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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
     * 通用 AI 对话（自由对话）
     * POST /api/ai/chat
     */
    @PostMapping("/chat")
    public Result<String> chat(@AuthenticationPrincipal Long userId,
                               @Valid @RequestBody AiChatRequest request) {
        String reply = aiService.chat(userId, request);
        return Result.ok(reply);
    }

    // ==================== 请求 DTO ====================

    @lombok.Data
    public static class SopAiReviewRequest {
        private Long sopId;
    }
}
