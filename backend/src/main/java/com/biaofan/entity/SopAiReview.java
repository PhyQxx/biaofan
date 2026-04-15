package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SOP AI 审核记录实体类
 * 记录每次发布前 AI 对 SOP 内容的审核结果
 */
@Data
@TableName("sop_ai_review")
public class SopAiReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** SOP ID */
    private Long sopId;
    /** SOP 版本号 */
    private Integer sopVersion;
    /** 审核模式：create / execute / review */
    private String reviewMode;
    /** AI 返回的原始 JSON */
    private String rawResponse;
    /** 审核结论：pass / warning / reject */
    private String verdict;
    /** 问题列表（JSON 数组） */
    private String issues;
    /** 改进建议（JSON 数组） */
    private String suggestions;
    /** 审核耗时（毫秒） */
    private Long costMs;
    /** 使用的模型类型 */
    private String modelType;
    private LocalDateTime createdAt;
}
