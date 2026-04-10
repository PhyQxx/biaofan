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

@RestController
@RequestMapping("/api/sop")
@RequiredArgsConstructor
public class SopVersionController {

    private final SopVersionService versionService;
    private final SopMapper sopMapper;

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
