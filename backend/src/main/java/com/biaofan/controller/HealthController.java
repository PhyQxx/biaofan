package com.biaofan.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查控制器
 * 提供系统健康状态检查接口，用于监控服务可用性
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 健康检查接口
     * @return 系统状态和当前时间戳
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis()
        );
    }
}
