package com.biaofan.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * AI 调用结果
 * 区分成功（content 非空）和失败（error 非空），避免静默空字符串
 */
@Data
@AllArgsConstructor
public class AiResult {
    /** 成功时的内容，null 表示失败 */
    private String content;
    /** 失败时的错误信息，null 表示成功 */
    private String error;

    public static AiResult success(String content) {
        return new AiResult(content, null);
    }

    public static AiResult error(String error) {
        return new AiResult(null, error);
    }

    public boolean isSuccess() {
        return error == null && content != null;
    }

    public String getContentOrEmpty() {
        return content != null ? content : "";
    }
}
