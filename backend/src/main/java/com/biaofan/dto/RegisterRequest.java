package com.biaofan.dto;

import lombok.Data;

/**
 * 用户注册请求参数封装
 * <p>包含用户注册所需的手机号、密码、用户名和验证码信息</p>
 */
@Data
public class RegisterRequest {
    /** 用户手机号，作为登录账号 */
    private String phone;
    /** 用户密码 */
    private String password;
    /** 用户昵称 */
    private String username;
    // H-12: 注册接口增加验证码字段
    /** 注册验证码 */
    private String code;
}
