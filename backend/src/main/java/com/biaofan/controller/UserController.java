package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.User;
import com.biaofan.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

    @PutMapping("/phone")
    public Result<Void> updatePhone(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        try {
            userService.updatePhone(userId, body.get("phone"));
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
