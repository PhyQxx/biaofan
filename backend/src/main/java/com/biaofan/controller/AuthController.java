package com.biaofan.controller;

import com.biaofan.dto.*;
import com.biaofan.entity.User;
import com.biaofan.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest req) {
        try {
            userService.register(req);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest req) {
        try {
            String token = userService.login(req);
            return Result.ok(Map.of("token", token));
        } catch (RuntimeException e) {
            return Result.fail(401, e.getMessage());
        }
    }

    @GetMapping("/me")
    public Result<User> me(@AuthenticationPrincipal Long userId) {
        User user = userService.getUserById(userId);
        if (user == null) return Result.fail(404, "用户不存在");
        user.setPassword(null);
        return Result.ok(user);
    }
}
