package com.biaofan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.dto.Result;
import com.biaofan.entity.AiModelConfig;
import com.biaofan.mapper.AiModelConfigMapper;
import com.biaofan.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员 AI 模型配置控制器
 * 管理全局 AI 配置（userId = null）
 */
@RestController
@RequestMapping("/api/admin/ai-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiConfigController {

    private final AiModelConfigMapper configMapper;
    private final AiService aiService;

    /**
     * 获取所有全局 AI 配置（userId = null）
     */
    @GetMapping("/global")
    public Result<Map<String, Object>> listGlobalConfigs() {
        List<AiModelConfig> list = configMapper.selectList(
            new LambdaQueryWrapper<AiModelConfig>()
                .isNull(AiModelConfig::getUserId)
                .orderByDesc(AiModelConfig::getUpdatedAt)
        );
        return Result.ok(Map.of("list", list, "total", list.size()));
    }

    /**
     * 获取所有用户级 AI 配置
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> listUserConfigs() {
        List<AiModelConfig> list = configMapper.selectList(
            new LambdaQueryWrapper<AiModelConfig>()
                .isNotNull(AiModelConfig::getUserId)
                .orderByDesc(AiModelConfig::getUpdatedAt)
        );
        return Result.ok(Map.of("list", list, "total", list.size()));
    }

    /**
     * 创建/更新全局 AI 配置
     */
    @PostMapping("/global")
    public Result<Void> saveGlobalConfig(@RequestBody AiModelConfig config) {
        aiService.saveGlobalConfig(config);
        return Result.ok();
    }

    /**
     * 删除全局 AI 配置
     */
    @DeleteMapping("/global/{id}")
    public Result<Void> deleteGlobalConfig(@PathVariable Long id) {
        aiService.deleteGlobalConfig(id);
        return Result.ok();
    }
}
