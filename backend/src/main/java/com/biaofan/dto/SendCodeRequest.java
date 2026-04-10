package com.biaofan.dto;

import lombok.Data;

/**
 * 发送验证码请求参数封装
 * <p>用于触发短信验证码发送功能</p>
 */
@Data
public class SendCodeRequest {
    /** 接收验证码的手机号 */
    private String phone;
}
