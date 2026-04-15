package com.biaofan.service;

import com.biaofan.dto.ai.SopAiAssistRequest;
import com.biaofan.dto.ai.SopAiCreateRequest;
import com.biaofan.entity.SopAiReview;

/**
 * SOP AI 辅助服务接口
 * 提供三大功能：AI 创建 SOP / AI 执行指导 / AI 发布审核
 */
public interface SopAiAssistService {

    /**
     * AI 辅助创建 SOP
     * 用户输入目标描述，AI 生成带步骤和检查项的结构化 SOP JSON
     *
     * @param userId 用户ID
     * @param request 请求（目标、分类等）
     * @return AI 生成的 SOP JSON 字符串（可直接写入 Sop.content）
     */
    String generateSop(Long userId, SopAiCreateRequest request);

    /**
     * AI 执行指导
     * 在步骤执行时提供实时指导
     *
     * @param userId 用户ID
     * @param request 当前步骤上下文
     * @return AI 指导文本
     */
    String getExecuteGuidance(Long userId, SopAiAssistRequest request);

    /**
     * AI 发布审核
     * SOP 发布前进行质量审核
     *
     * @param userId 用户ID
     * @param sopId SOP ID
     * @param sopContentJson SOP 内容 JSON
     * @param sopVersion SOP 版本号
     * @return 审核结果实体
     */
    SopAiReview reviewSop(Long userId, Long sopId, String sopContentJson, Integer sopVersion);
}
