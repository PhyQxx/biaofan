package com.biaofan.dto.ai;

import lombok.Data;
import java.util.Map;

/**
 * SOP AI 执行辅助请求
 */
@Data
public class SopAiAssistRequest {
    /** 当前步骤标题 */
    private String stepTitle;
    /** 当前步骤描述 */
    private String stepDescription;
    /** 当前步骤索引 */
    private Integer stepIndex;
    /** 步骤总数量 */
    private Integer totalSteps;
    /** 用户在当前步骤输入的检查数据 */
    private Map<String, Object> checkData;
    /** 用户备注 */
    private String notes;
    /** 模型类型 */
    private String modelType;
    /** AI 指导结果（步骤完成时关联保存） */
    private String guidance;
}
