package com.biaofan.controller;

import com.biaofan.dto.*;
import com.biaofan.entity.User;
import com.biaofan.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final int CODE_VALID_MINUTES = 5;

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

    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestBody SendCodeRequest req) {
        String phone = req.getPhone();
        
        // 手机号格式校验 - 中国大陆 11 位
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return Result.fail(400, "手机号格式不正确");
        }
        
        // 生成 6 位随机验证码
        String code = String.format("%06d", new Random().nextInt(1000000));
        
        // 存入 Redis，5 分钟有效
        String key = SMS_CODE_PREFIX + phone;
        redisTemplate.opsForValue().set(key, code, CODE_VALID_MINUTES, TimeUnit.MINUTES);
        
        // TODO: 实际生产环境应调用短信网关发送验证码
        // 此处仅打印日志，实际短信发送需接入阿里云/腾讯云等短信服务
        System.out.println("[SMS] 发送验证码 to " + phone + ", code: " + code);
        
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<User> me(@AuthenticationPrincipal Long userId) {
        User user = userService.getUserById(userId);
        if (user == null) return Result.fail(404, "用户不存在");
        user.setPassword(null);
        return Result.ok(user);
    }
}
