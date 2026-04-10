package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopVersion;
import com.biaofan.mapper.SopMapper;
import com.biaofan.service.SopVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SOP版本控制器
 * <p>提供SOP版本历史查询、查看指定版本、回滚到指定版本等功能。
 * 每次SOP发布（publish）时会生成新的版本记录。</p>
 *
 * @RestController
 * @RequestMapping("/api/sop")
 * @RequiredArgsConstructor
 */
@RestController
@RequestMapping("/api/sop")
@RequiredArgsConstructor
public class SopVersionController {

    private final SopVersionService versionService;
    private final SopMapper sopMapper;

    /**
     * 获取SOP的版本历史列表
     * <p>返回指定SOP所有历史版本的列表，按版本号降序排列。</p>
     *
     * @param id     SOP ID
     * @param userId 当前登录用户ID（校验是否为SOP所有者）
     * @return 版本列表
     */
    // H-11: versions() 增加所有者校验
    @GetMapping("/{id}/versions")
    public Result<List<SopVersion>> versions(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        Sop sop = sopMapper.selectById(id);
        if (sop == null) return Result.fail(404, "SOP不存在");
        if (!sop.getUserId().equals(userId)) return Result.fail(403, "无权访问");
        return Result.ok(versionService.getVersions(id));
    }

    /**
     * 获取SOP指定版本的详情
     *
     * @param id      SOP ID
     * @param version 版本号
     * @param userId  当前登录用户ID（校验是否为SOP所有者）
     * @return 版本详情
     */
    @GetMapping("/{id}/versions/{version}")
    public Result<SopVersion> version(
            @PathVariable Long id,
            @PathVariable Integer version,
            @AuthenticationPrincipal Long userId) {
        try {
            // H-11: 增加所有者校验
            Sop sop = sopMapper.selectById(id);
            if (sop == null) return Result.fail(404, "SOP不存在");
            if (!sop.getUserId().equals(userId)) return Result.fail(403, "无权访问");
            return Result.ok(versionService.getVersion(id, version));
        } catch (RuntimeException e) {
            return Result.fail(404, e.getMessage());
        }
    }

    /**
     * 回滚SOP到指定版本
     * <p>将SOP内容恢复为指定历史版本的内容，同时生成新的版本记录。</p>
     *
     * @param id      SOP ID
     * @param version 目标版本号
     * @param userId  当前登录用户ID（校验是否为SOP所有者）
     * @return 操作结果
     */
    @PostMapping("/{id}/rollback/{version}")
    public Result<Void> rollback(
            @PathVariable Long id,
            @PathVariable Integer version,
            @AuthenticationPrincipal Long userId) {
        try {
            versionService.rollback(id, userId, version);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
