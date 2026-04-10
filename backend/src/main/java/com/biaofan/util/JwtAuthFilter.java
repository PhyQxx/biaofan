package com.biaofan.util;


/**
 * JWT 认证过滤器
 * - 继承 OncePerRequestFilter，每请求执行一次
 * - 从请求头 Authorization: Bearer {token} 中提取 JWT
 * - 调用 JwtUtil 验证 token，解析出 userId 和 role
 * - 将用户信息存入 SecurityContextHolder，供下游使用
 * - 验证失败或无 token 时放行（由 SecurityConfig 中的路径规则决定是否拦截）
 */
import com.biaofan.entity.User;
import com.biaofan.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT认证过滤器，基于OncePerRequestFilter实现。
 * 从HTTP请求的Authorization头中解析Bearer Token，验证通过后设置Spring Security上下文。
 *
 * @author biaofan
 * @see OncePerRequestFilter
 * @see JwtUtil
 */

/**
 * JWT 认证过滤器
 * - 继承 OncePerRequestFilter，每请求执行一次
 * - 从请求头 Authorization: Bearer {token} 中提取 JWT
 * - 调用 JwtUtil 验证 token，解析出 userId 和 role
 * - 将用户信息存入 SecurityContextHolder，供下游使用
 * - 验证失败或无 token 时放行（由 SecurityConfig 中的路径规则决定是否拦截）
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    /**
     * 核心过滤方法，解析并验证请求中的JWT令牌。
     * 验证通过后，将用户信息及权限封装到Spring Security上下文中的认证对象里。
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @param chain    过滤器链
     * @throws ServletException 如果处理请求时发生Servlet错误
     * @throws IOException      如果处理请求时发生I/O错误
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = parseToken(request);
        if (token != null && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserId(token);
            if (userId != null) {
                User user = userService.getUserById(userId);
                if (user != null) {
                    List<String> roles = user.getRole() != null
                        ? List.of(user.getRole().split(","))
                        : Collections.emptyList();
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()))
                        .toList();
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 从请求头中解析Bearer Token。
     *
     * @param request HTTP请求对象
     * @return token字符串，若不存在或格式不正确则返回null
     */
    private String parseToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
