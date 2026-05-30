package com.biaofan.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

/**
 * AI 调用结果
 * 区分成功（content/embedding 非空）和失败（error 非空）
 */
@Data
@AllArgsConstructor
public class AiResult {
    /** 文本对话成功时的内容 */
    private String content;
    /** 向量生成成功时的内容 */
    private List<Double> embedding;
    /** 失败时的错误信息，null 表示成功 */
    private String error;

    public static AiResult success(String content) {
        return new AiResult(content, null, null);
    }

    public static AiResult success(List<Double> embedding) {
        return new AiResult(null, embedding, null);
    }

    public static AiResult error(String error) {
        return new AiResult(null, null, error);
    }

    public boolean isSuccess() {
        return error == null && (content != null || embedding != null);
    }

    public String getContentOrEmpty() {
        return content != null ? content : "";
    }
}
