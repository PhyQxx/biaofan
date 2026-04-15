package com.biaofan.dto.ai;

import lombok.Data;

/**
 * SOP AI 辅助创建请求
 */
@Data
public class SopAiCreateRequest {
    /** 用户描述的目标或场景 */
    private String goal;
    /** SOP 标题（AI 生成后可覆盖） */
    private String title;
    /** 分类 */
    private String category;
    /** 标签列表 */
    private String tags;
    /** 模型类型 */
    private String modelType;
}
