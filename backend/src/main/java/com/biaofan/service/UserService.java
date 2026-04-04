package com.biaofan.service;

import com.biaofan.entity.User;
import com.biaofan.dto.LoginRequest;
import com.biaofan.dto.RegisterRequest;

public interface UserService {
    String login(LoginRequest req);
    void register(RegisterRequest req);
    User getUserById(Long id);
    User getUserByPhone(String phone);
}
