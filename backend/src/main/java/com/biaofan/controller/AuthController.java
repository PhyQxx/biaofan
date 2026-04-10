package com.biaofan.controller;

import com.biaofan.dto.*;
import com.biaofan.entity.User;
import com.biaofan.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String SMS_RATE_PREFIX = "sms:rate:";
    private static final int CODE_VALID_MINUTES = 5;
    // H-02: 使用 SecureRandom 替代 new Random()
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest req) {
        try {
            // H-12: 注册接口增加验证码校验
            if (req.getPhone() == null || req.getCode() == null) {
                return Result.fail(400, "手机号和验证码不能为空");
            }
            String key = SMS_CODE_PREFIX + req.getPhone();
            String cachedCode = redisTemplate.opsForValue().get(key);
            if (cachedCode == null || !cachedCode.equals(req.getCode())) {
                return Result.fail(400, "验证码错误或已过期");
            }
            // 验证通过后删除验证码，防止重复使用
            redisTemplate.delete(key);
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

        // H-03: Redis 限流 60s 内只能发送一次
        String rateKey = SMS_RATE_PREFIX + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateKey))) {
            return Result.fail(429, "验证码发送过于频繁，请60秒后重试");
        }
        
        // H-02: 使用 SecureRandom 生成 6 位随机验证码
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        
        // 存入 Redis，5 分钟有效
        String key = SMS_CODE_PREFIX + phone;
        redisTemplate.opsForValue().set(key, code, CODE_VALID_MINUTES, TimeUnit.MINUTES);

        // H-03: 设置限流 key，60 秒有效
        redisTemplate.opsForValue().set(rateKey, "1", 60, TimeUnit.SECONDS);
        
        // TODO: 实际生产环境应调用短信网关发送验证码
        // H-01: 仅打印脱敏手机号，不打印验证码明文
        String maskedPhone = phone.substring(0, 3) + "****" + phone.substring(7);
        System.out.println("[SMS] 发送验证码 to " + maskedPhone);
        
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
