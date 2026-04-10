package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.User;
import com.biaofan.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户信息控制器
 * 提供用户个人资料的修改功能，包括用户名、密码、手机号更新
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 更新用户名
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @param body 请求体，包含新用户名username
     * @return 操作结果
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        try {
            userService.updateProfile(userId, body.get("username"));
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 修改密码
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @param body 请求体，包含旧密码oldPassword和新密码newPassword
     * @return 操作结果
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        try {
            userService.updatePassword(userId, body.get("oldPassword"), body.get("newPassword"));
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 更新手机号
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @param body 请求体，包含新手机号phone和验证码code
     * @return 操作结果
     */
    @PutMapping("/phone")
    public Result<Void> updatePhone(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        try {
            // H-13: updatePhone 需要验证码
            userService.updatePhone(userId, body.get("phone"), body.get("code"));
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
