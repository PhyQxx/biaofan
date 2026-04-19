package com.biaofan.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * API Version Filter
 * 
 * KNOWN LIMITATION: This project currently lacks proper API versioning.
 * All endpoints use /api/ prefix without version identification.
 * 
 * Migration plan (documented for future refactor):
 * 1. All controllers change @RequestMapping("/api/sop") → @RequestMapping("/api/v1/sop")
 * 2. New @RequestMapping("/api/v2/...") endpoints added for breaking changes
 * 3. Old /api/... routes emit deprecation headers
 * 
 * This filter provides interim deprecation signaling to clients.
 */
@Component
@WebFilter(urlPatterns = "/api/*")
public class ApiVersionFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) res;
        resp.setHeader("X-API-Version", "1.0");
        resp.setHeader("X-API-Deprecation", "true; sunset=2025-12-31; message=migrate to /api/v1/ path");
        chain.doFilter(req, res);
    }
}
