package com.biaofan.service;

import com.biaofan.entity.User;
import com.biaofan.dto.LoginRequest;
import com.biaofan.dto.RefreshTokenRequest;
import com.biaofan.dto.RegisterRequest;

import java.util.Map;

/**
 * 用户服务接口
 * 提供用户登录、注册、个人信息管理等功能
 */
public interface UserService {
    /**
     * 用户登录
     * @param req 登录请求（包含手机号、密码等）
     * @return 登录成功返回token凭证
     */
    String login(LoginRequest req);

    /**
     * 用户登录（带刷新令牌）
     * @param req 登录请求（包含手机号、密码等）
     * @return 登录结果，包含token、refreshToken和expiresIn
     */
    Map<String, Object> loginWithRefreshToken(LoginRequest req);

    /**
     * 刷新访问令牌
     * @param refreshToken 刷新令牌
     * @return 新的token、refreshToken和expiresIn
     */
    Map<String, Object> refreshToken(String refreshToken);

    /**
     * 用户注册
     * @param req 注册请求（包含手机号、密码等）
     */
    void register(RegisterRequest req);

    /**
     * 根据用户ID获取用户信息
     * @param id 用户ID
     * @return 用户实体
     */
    User getUserById(Long id);

    /**
     * 根据手机号获取用户信息
     * @param phone 手机号
     * @return 用户实体
     */
    User getUserByPhone(String phone);

    /**
     * 更新用户昵称
     * @param userId 用户ID
     * @param username 新昵称
     */
    void updateProfile(Long userId, String username);

    /**
     * 修改密码（需验证旧密码）
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 更新手机号（需验证短信验证码）
     * @param userId 用户ID
     * @param phone 新手机号
     * @param code 短信验证码
     */
    void updatePhone(Long userId, String phone, String code);

    /**
     * 更新手机号（直接更新，无验证码校验）
     * @param userId 用户ID
     * @param phone 新手机号
     */
    void updatePhone(Long userId, String phone);
}
