package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.AiModelConfig;
import com.biaofan.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * AI 模型配置接口
 */
@RestController
@RequestMapping("/api/ai/config")
@RequiredArgsConstructor
public class AiModelConfigController {

    private final AiService aiService;

    /**
     * 获取当前用户的 AI 配置
     */
    @GetMapping
    public Result<AiModelConfig> getMyConfig(@AuthenticationPrincipal Long userId) {
        AiModelConfig config = aiService.getUserConfig(userId);
        return Result.ok(config);
    }

    /**
     * 保存/更新 AI 配置
     */
    @PostMapping
    public Result<Void> saveConfig(@AuthenticationPrincipal Long userId,
                                   @RequestBody AiModelConfig config) {
        aiService.saveConfig(userId, config);
        return Result.ok();
    }

    /**
     * 删除用户 AI 配置（恢复全局默认）
     */
    @DeleteMapping
    public Result<Void> deleteConfig(@AuthenticationPrincipal Long userId) {
        aiService.deleteConfig(userId);
        return Result.ok();
    }
}
