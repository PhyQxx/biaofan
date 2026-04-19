package com.biaofan.config;

import com.biaofan.util.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**

/**
 * Spring Security 安全配置
 * - 禁用 CSRF（前后端分离项目）
 * - 放开 /api/auth/**、/api/sop/templates/list、/api/marketplace/** 的访问权限
 * - ADMIN 角色拥有 ROLE_ADMIN 权限
 * - 其他请求需要登录认证
 * - 将 JwtAuthFilter 添加到过滤器链
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Lazy
    private final JwtAuthFilter jwtAuthFilter;

    private final Environment environment;

    /**
     * 配置安全过滤器链
     * <p>包含以下配置：
     * <ul>
     *   <li>CORS 跨域配置</li>
     *   <li>CSRF 禁用（前后端分离场景）</li>
     *   <li>无状态 Session（JWT 场景）</li>
     *   <li>路径授权规则：公开路径放行，/api/admin/** 需 ADMIN 角色</li>
     *   <li>JWT 认证过滤器优先于用户名密码认证过滤器执行</li>
     * </ul>
     *
     * @param http HttpSecurity 配置对象
     * @return SecurityFilterChain 实例
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable) // JWT in Authorization header — CSRF not applicable for stateless API
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/send-code", "/api/auth/refresh", "/api/auth/login-with-refresh", "/api/health").permitAll()
                // C-02: /api/gamification/** 已删除 permitAll，走 authenticated()
                // C-01: /api/admin/** 需要 ADMIN 角色
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // SOP 公开接口（无需登录即可查看）
                .requestMatchers("/api/sop/templates", "/api/sop/categories", "/api/sop/category", "/api/sop/public/**").permitAll()
                .requestMatchers("/api/ai/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS 跨域配置
     * <p>允许所有来源（仅用于开发环境），支持常用 HTTP 方法和所有请求头
     * <p><strong>注意：</strong>生产环境应将 allowedOriginPatterns 替换为具体域名
     *
     * @return CorsConfigurationSource 实例
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        String corsOriginsEnv = environment.getProperty("cors.origins", "");
        if (!StringUtils.hasText(corsOriginsEnv)) {
            corsOriginsEnv = System.getenv("CORS_ORIGINS");
        }
        List<String> allowedOrigins = new ArrayList<>();
        if (StringUtils.hasText(corsOriginsEnv)) {
            allowedOrigins.addAll(Arrays.asList(corsOriginsEnv.split(",")));
        }

        // If empty and in dev profile, allow localhost for development
        if (allowedOrigins.isEmpty()) {
            String[] activeProfiles = environment.getActiveProfiles();
            String springProfile = activeProfiles.length > 0 ? activeProfiles[0] : "";
            if ("dev".equals(springProfile) || "development".equals(springProfile)) {
                allowedOrigins.add("http://localhost:*");
                // Note: Use concrete port in production, wildcard only for dev convenience
                log.warn("CORS_ORIGINS not set, defaulting to localhost:* for development only");
            } else {
                // Production: log error but allow empty (browser will block, which is the safe default)
                log.error("CORS_ORIGINS not configured for non-dev profile!");
            }
        }
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
