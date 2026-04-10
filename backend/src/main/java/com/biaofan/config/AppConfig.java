package com.biaofan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 通用应用配置
 * 将 BCryptPasswordEncoder 从 SecurityConfig 移出，打破循环依赖：
 * jwtAuthFilter → userServiceImpl → securityConfig → jwtAuthFilter
 */
@Configuration
public class AppConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
