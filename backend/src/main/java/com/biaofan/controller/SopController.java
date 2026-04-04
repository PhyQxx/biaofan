package com.biaofan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.biaofan.dto.Result;
import com.biaofan.dto.SopRequest;
import com.biaofan.entity.Sop;
import com.biaofan.service.SopService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sop")
@RequiredArgsConstructor
public class SopController {

    private final SopService sopService;

    @GetMapping("/my")
    public Result<IPage<Sop>> mySops(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(sopService.getMySops(userId, page, size));
    }

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

    @PostMapping("/{id}/publish")
    public Result<Void> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        try {
            sopService.publish(id, userId);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
