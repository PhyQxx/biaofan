package com.biaofan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.dto.Result;
import com.biaofan.dto.SopRequest;
import com.biaofan.entity.Sop;
import com.biaofan.mapper.SopMapper;
import com.biaofan.service.SopService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SOP模板控制器
 * <p>提供SOP模板的浏览、创建、更新、删除等管理功能。
 * 公开接口仅返回已发布（published）状态的模板。</p>
 *
 * @RestController
 * @RequestMapping("/api/sop/templates")
 * @RequiredArgsConstructor
 */
@RestController
@RequestMapping("/api/sop/templates")
@RequiredArgsConstructor
public class SopTemplateController {

    private final SopService sopService;
    private final SopMapper sopMapper;

    /**
     * 获取模板列表
     * <p>公开接口，仅返回已发布的模板，支持按分类和标签过滤。</p>
     *
     * @param category 可选的分类过滤
     * @param tag      可选的标签过滤
     * @return 模板列表（仅已发布状态）
     */
    /** 模板列表（支持分类/标签过滤）- H-09: 公开接口强制只查 published 状态 */
    @GetMapping
    public Result<List<Sop>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag) {
        LambdaQueryWrapper<Sop> q = new LambdaQueryWrapper<Sop>();
        if (category != null && !category.isBlank()) q.eq(Sop::getCategory, category);
        // H-09: 公开接口强制只展示已发布的模板，忽略客户端传入的 status 参数
        q.eq(Sop::getStatus, "published");
        q.orderByDesc(Sop::getUpdatedAt);
        List<Sop> list = sopMapper.selectList(q);
        // Tag filtering done in memory (tags stored as JSON)
        if (tag != null && !tag.isBlank()) {
            list = list.stream()
                .filter(s -> s.getTags() != null && s.getTags().contains(tag))
                .toList();
        }
        return Result.ok(list);
    }

    /**
     * 创建模板
     *
     * @param userId 当前登录用户ID
     * @param req    模板创建请求体
     * @return 创建结果
     */
    /** 创建模板 */
    @PostMapping
    public Result<Void> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody SopRequest req) {
        try {
            sopService.create(userId, req);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 更新模板
     *
     * @param id     模板ID
     * @param userId 当前登录用户ID（校验是否为模板所有者）
     * @param req    模板更新请求体
     * @return 更新结果
     */
    /** 更新模板 */
    @PutMapping("/{id}")
    public Result<Void> update(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestBody SopRequest req) {
        try {
            sopService.update(id, userId, req);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 删除模板
     *
     * @param id     模板ID
     * @param userId 当前登录用户ID（校验是否为模板所有者）
     * @return 删除结果
     */
    /** 删除模板 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        try {
            sopService.delete(id, userId);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(404, e.getMessage());
        }
    }
}
