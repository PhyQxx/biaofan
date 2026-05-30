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
 * SOP（标准操作程序）管理接口
 * 提供SOP的分页查询、详情获取、创建、更新、删除及发布功能
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
     * @param orgId  组织ID（可选，为null表示个人空间）
     * @param page   页码（默认1）
     * @param size   每页条数（默认20）
     * @return 当前用户的SOP分页列表
     */
    @com.biaofan.constant.annotation.CheckOrg
    @GetMapping("/my")
    public Result<IPage<Sop>> mySops(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (size > 100) size = 100;
        if (page < 1) page = 1;
        return Result.ok(sopService.getMySops(userId, orgId, page, size));
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
     * @param orgId  组织ID（可选，为null表示个人空间）
     * @param req    SOP创建请求体
     * @return 创建成功后的SOP信息
     */
    @PostMapping
    public Result<Sop> create(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long orgId,
            @Valid @RequestBody SopRequest req) {
        try {
            Sop sop = sopService.create(userId, orgId, req);
            return Result.created(sop);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 更新已有SOP的内容
     *
     * @param id     SOP ID
     * @param userId 当前登录用户ID
     * @param req    SOP更新内容
     * @return 更新后的SOP信息（暂返回200 OK）
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
     * 删除指定的SOP（软删除由Service控制）
     *
     * @param id     SOP ID
     * @param userId 当前登录用户ID
     * @return 操作结果
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
     * 发布SOP，使其可被执行
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

    /**
     * 将个人 SOP 转移至组织
     *
     * @param id    SOP ID
     * @param orgId 目标组织ID
     * @param userId 当前用户ID
     * @return 操作结果
     */
    @PostMapping("/{id}/transfer")
    public Result<Void> transfer(
            @PathVariable Long id,
            @RequestParam Long orgId,
            @AuthenticationPrincipal Long userId) {
        try {
            sopService.transferToOrganization(id, userId, orgId);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
