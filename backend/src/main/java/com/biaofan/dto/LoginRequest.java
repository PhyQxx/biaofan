package com.biaofan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户登录请求参数封装
 * <p>包含用户登录所需的手机号和密码信息</p>
 */
@Data
public class LoginRequest {
    /** 用户手机号，作为登录账号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;
    /** 用户密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;
}
