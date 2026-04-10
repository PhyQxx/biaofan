package com.biaofan.dto;

import lombok.Data;

/**
 * 统一API响应结果封装类
 * <p>用于所有API接口的标准化返回格式，包含状态码、消息、数据和成功标识</p>
 *
 * @param <T> 响应数据的泛型类型
 */
@Data
public class Result<T> {
    /** 状态码，200表示成功，500表示失败，其他值表示特定业务错误 */
    private int code;
    /** 响应消息，用于描述操作结果或错误原因 */
    private String message;
    /** 响应数据，泛型类型，根据具体接口返回不同的数据 */
    private T data;
    /** 时间戳，记录响应生成的时间（毫秒） */
    private long timestamp;
    /** 操作是否成功标识，true表示成功，false表示失败 */
    private boolean success;

    /**
     * 成功响应（带数据）
     *
     * @param data 响应的数据
     * @param <T>  数据类型
     * @return Result实例
     */
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "操作成功";
        r.data = data;
        r.timestamp = System.currentTimeMillis();
        r.success = true;
        return r;
    }

    /**
     * 成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return Result实例
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 失败响应（默认错误码500）
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return Result实例
     */
    public static <T> Result<T> fail(String message) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.message = message;
        r.timestamp = System.currentTimeMillis();
        r.success = false;
        return r;
    }

    /**
     * 失败响应（自定义错误码）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return Result实例
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.timestamp = System.currentTimeMillis();
        r.success = false;
        return r;
    }
}
