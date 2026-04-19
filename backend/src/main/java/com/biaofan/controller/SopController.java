package com.biaofan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.biaofan.dto.Result;
import com.biaofan.dto.SopRequest;
import com.biaofan.entity.Sop;
import com.biaofan.service.SopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * SOP（标准作业程序）控制器
 * <p>提供SOP的增删改查、发布等功能，供已登录用户管理自己的SOP。</p>
 *
 * @RestController
 * @RequestMapping("/api/sop")
 * @RequiredArgsConstructor
 */
@RestController
@RequestMapping("/api/sop")
@RequiredArgsConstructor
public class SopController {

    private final SopService sopService;

    /**
     * 获取当前用户的SOP列表（分页）
     *
     * @param userId 当前登录用户ID
     * @param page   页码（默认1）
     * @param size   每页条数（默认20）
     * @return 当前用户的SOP分页列表
     */
    @GetMapping("/my")
    public Result<IPage<Sop>> mySops(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(sopService.getMySops(userId, page, size));
    }

    /**
     * 根据ID获取单个SOP详情
     *
     * @param id     SOP ID
     * @param userId 当前登录用户ID（用于权限校验）
     * @return SOP详情
     */
    @GetMapping("/{id}")
    public Result<Sop> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        try {
            return Result.ok(sopService.getById(id, userId));
        } catch (RuntimeException e) {
            return Result.fail(404, e.getMessage());
        }
    }

    /**
     * 创建新的SOP
     *
     * @param userId 当前登录用户ID
     * @param req    SOP创建请求体
     * @return 创建成功后的SOP信息
     */
    @PostMapping
    public Result<Sop> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SopRequest req) {
        try {
            Sop sop = sopService.create(userId, req);
            return Result.created(sop);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 更新指定SOP
     *
     * @param id     SOP ID
     * @param userId 当前登录用户ID（校验是否为SOP所有者）
     * @param req    SOP更新请求体
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<Void> update(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SopRequest req) {
        try {
            sopService.update(id, userId, req);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 删除指定SOP
     *
     * @param id     SOP ID
     * @param userId 当前登录用户ID（校验是否为SOP所有者）
     * @return 删除结果
     */
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

    /**
     * 发布SOP
     * <p>将SOP状态设为已发布（published），可选择传入变更摘要。</p>
     *
     * @param id     SOP ID
     * @param userId 当前登录用户ID
     * @param req    可选的变更摘要请求体
     * @return 发布结果
     */
    @PostMapping("/{id}/publish")
    public Result<Void> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestBody(required = false) SopRequest req) {
        try {
            if (req != null && req.getChangeSummary() != null) {
                sopService.publish(id, userId, req.getChangeSummary());
            } else {
                sopService.publish(id, userId);
            }
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
