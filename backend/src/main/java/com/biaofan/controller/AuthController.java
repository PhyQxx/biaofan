package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.dto.RefreshTokenRequest;
import com.biaofan.dto.LoginRequest;
import com.biaofan.dto.RegisterRequest;
import com.biaofan.dto.SendCodeRequest;
import com.biaofan.entity.User;
import com.biaofan.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证控制器
 * <p>提供用户注册、登录、短信验证码发送、当前用户信息查询等功能。</p>
 *
 * @RestController
 * @RequestMapping("/api/auth")
 * @RequiredArgsConstructor
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String SMS_RATE_PREFIX = "sms:rate:";
    private static final String LOGIN_RATE_KEY = "login_rate:";
    private static final int LOGIN_RATE_LIMIT = 10; // per minute
    private static final Duration LOGIN_RATE_WINDOW = Duration.ofMinutes(1);
    private static final int CODE_VALID_MINUTES = 5;
    // H-02: 使用 SecureRandom 替代 new Random()
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 用户注册
     * <p>接收手机号和短信验证码，验证通过后创建用户账号。</p>
     *
     * @param req 注册请求体（包含手机号、验证码、密码等）
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
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

    /**
     * 用户登录
     * <p>使用手机号和密码登录，验证通过后返回JWT Token。</p>
     *
     * @param req 登录请求体（包含手机号、密码）
     * @return 登录结果，包含token
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String rateKey = LOGIN_RATE_KEY + ip;

        try {
            Long attempts = redisTemplate.opsForValue().increment(rateKey);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(rateKey, LOGIN_RATE_WINDOW);
            }
            if (attempts != null && attempts > LOGIN_RATE_LIMIT) {
                return Result.fail(429, "请求过于频繁，请稍后再试");
            }
        } catch (Exception e) {
            // Redis down — allow request to proceed
            log.warn("Rate limit check failed, allowing request", e);
        }

        try {
            String token = userService.login(req);
            return Result.ok(Map.of("token", token));
        } catch (RuntimeException e) {
            return Result.fail(401, e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 用户登录（带刷新令牌）
     * <p>使用手机号和密码登录，验证通过后返回JWT Token和刷新令牌。</p>
     *
     * @param req 登录请求体（包含手机号、密码）
     * @return 登录结果，包含token、refreshToken和expiresIn
     */
    @PostMapping("/login-with-refresh")
    public Result<Map<String, Object>> loginWithRefresh(@Valid @RequestBody LoginRequest req) {
        try {
            Map<String, Object> result = userService.loginWithRefreshToken(req);
            return Result.ok(result);
        } catch (RuntimeException e) {
            return Result.fail(401, e.getMessage());
        }
    }

    /**
     * 刷新访问令牌
     * <p>使用刷新令牌获取新的访问令牌和刷新令牌（旋转机制）。</p>
     *
     * @param req 刷新令牌请求体（包含refreshToken）
     * @return 新的token、refreshToken和expiresIn
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        try {
            Map<String, Object> result = userService.refreshToken(req.getRefreshToken());
            return Result.ok(result);
        } catch (RuntimeException e) {
            return Result.fail(401, e.getMessage());
        }
    }

    /**
     * 发送短信验证码
     * <p>向指定手机号发送6位数字验证码，验证码有效期5分钟。
     * 同一手机号60秒内只能发送一次。</p>
     *
     * @param req 发送验证码请求体（包含手机号）
     * @return 发送结果
     */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest req) {
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
        log.info("[SMS] 发送验证码 to {}", maskedPhone);
        
        return Result.ok();
    }

    /**
     * 获取当前登录用户信息
     * <p>通过Spring Security的@AuthenticationPrincipal获取当前用户ID并查询用户信息。</p>
     *
     * @param userId 当前登录用户的ID（从Token中解析）
     * @return 用户信息（密码已置空）
     */
    @GetMapping("/me")
    public Result<User> me(@AuthenticationPrincipal Long userId) {
        User user = userService.getUserById(userId);
        if (user == null) return Result.fail(404, "用户不存在");
        user.setPassword(null);
        return Result.ok(user);
    }
}
