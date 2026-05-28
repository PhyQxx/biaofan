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

    /**
     * AI 步骤智能补全
     * 根据当前 SOP 的标题和已存在的步骤，预测后续的 3 个逻辑步骤
     *
     * @param userId 用户ID
     * @param title SOP 标题
     * @param existingStepsJson 当前已有的步骤 JSON
     * @return 预测的步骤 JSON 数组字符串
     */
    String predictNextSteps(Long userId, String title, String existingStepsJson);

    /**
     * 组织级智能 Q&A
     * 基于组织内所有 SOP 知识回答用户提问
     *
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param question 用户提问
     * @return AI 回答内容
     */
    String getOrgQa(Long userId, Long orgId, String question);
}
