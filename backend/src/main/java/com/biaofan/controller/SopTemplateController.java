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

@RestController
@RequestMapping("/api/sop/templates")
@RequiredArgsConstructor
public class SopTemplateController {

    private final SopService sopService;
    private final SopMapper sopMapper;

    /** 模板列表（支持分类/标签过滤） */
    @GetMapping
    public Result<List<Sop>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Sop> q = new LambdaQueryWrapper<Sop>();
        if (category != null && !category.isBlank()) q.eq(Sop::getCategory, category);
        if (status != null && !status.isBlank()) q.eq(Sop::getStatus, status);
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
