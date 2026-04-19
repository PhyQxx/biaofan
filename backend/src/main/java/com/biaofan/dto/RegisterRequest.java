package com.biaofan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户注册请求参数封装
 * <p>包含用户注册所需的手机号、密码、用户名和验证码信息</p>
 */
@Data
public class RegisterRequest {
    /** 用户手机号，作为登录账号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;
    /** 用户密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
    /** 用户昵称 */
    @NotBlank(message = "昵称不能为空")
    private String username;
    // H-12: 注册接口增加验证码字段
    /** 注册验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
