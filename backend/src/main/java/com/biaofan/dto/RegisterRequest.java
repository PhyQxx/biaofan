package com.biaofan.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String phone;
    private String password;
    private String username;
    // H-12: 注册接口增加验证码字段
    private String code;
}
