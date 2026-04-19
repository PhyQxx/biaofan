package com.biaofan.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类，提供JWT令牌的生成、解析和验证功能。
 * 依赖于配置文件中的jwt.secret和jwt.ttl属性。
 *
 * @author biaofan
 * @see <a href="https://github.com/jwtk/jjwt">JJWT</a>
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.ttl}")
    private Long ttl;

    /**
     * 获取用于签名和验证的密钥对象。
     *
     * @return SecretKey 用于JWT签名验证的密钥
     */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT令牌。
     *
     * @param userId   用户的唯一标识ID
     * @param username 用户名
     * @return 生成的JWT令牌字符串
     */
    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttl))
                .signWith(getKey())
                .compact();
    }

    // H-14/M-13: getUserId() 不再吞异常，增加日志记录
    /**
     * 从JWT令牌中解析提取用户ID。
     *
     * @param token JWT令牌字符串
     * @return 用户ID，若令牌过期或解析失败则返回null
     */
    public Long getUserId(String token) {
        try {
            return Long.parseLong(Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject());
        } catch (ExpiredJwtException e) {
            // JWT 已过期但签名仍有效，此时 claims 仍可读
            String subject = e.getClaims().getSubject();
            if (subject != null) {
                return Long.parseLong(subject);
            }
            log.warn("JWT token 已过期且无法解析userId: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的 JWT token: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.warn("JWT token 格式错误: {}", e.getMessage());
            return null;
        } catch (SecurityException e) {
            log.warn("JWT token 签名无效: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token 为空: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("解析 JWT token 异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 验证JWT令牌的签名有效性。已过期的token将被拒绝。
     *
     * @param token JWT令牌字符串
     * @return true表示签名有效且未过期，false表示签名无效、格式错误或已过期
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token 已过期: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("JWT token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证JWT令牌的签名有效性，允许已过期的token（用于refresh token场景）。
     *
     * @param token JWT令牌字符串
     * @return true表示签名有效（包含已过期），false表示签名无效或格式错误
     */
    public boolean validateTokenWithoutExpiry(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token 已过期但签名有效: {}", e.getMessage());
            return true;
        } catch (JwtException e) {
            log.warn("JWT token 验证失败: {}", e.getMessage());
            return false;
        }
    }
}
