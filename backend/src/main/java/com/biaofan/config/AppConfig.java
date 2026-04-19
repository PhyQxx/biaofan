package com.biaofan.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 通用应用配置类
 * <p>将 BCryptPasswordEncoder 从 SecurityConfig 移出，打破循环依赖：
 * <br>jwtAuthFilter → userServiceImpl → securityConfig → jwtAuthFilter
 *
 * @author biaofan
 * @see BCryptPasswordEncoder
 */
@Configuration
public class AppConfig {

    /**
     * 注册 BCryptPasswordEncoder Bean
     * <p>用于密码加密与校验，采用 BCrypt 强哈希算法
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // strength 12 (2^12 iterations)
    }

    /**
     * Shared ObjectMapper bean for JSON serialization/deserialization
     * Configured with Java 8 time module support
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
