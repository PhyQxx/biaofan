package com.biaofan.dto;

import lombok.Data;

/**
 * 用户登录请求参数封装
 * <p>包含用户登录所需的手机号和密码信息</p>
 */
@Data
public class LoginRequest {
    /** 用户手机号，作为登录账号 */
    private String phone;
    /** 用户密码 */
    private String password;
}
