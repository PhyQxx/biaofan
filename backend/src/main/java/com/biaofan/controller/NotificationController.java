package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.Notification;
import com.biaofan.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 * 提供用户通知的查询、标记已读等功能
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取当前用户的通知列表
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 通知列表
     */
    @GetMapping
    public Result<List<Notification>> list(@AuthenticationPrincipal Long userId) {
        return Result.ok(notificationService.getMyNotifications(userId));
    }

    /**
     * 获取未读通知数量
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 未读数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> unreadCount(@AuthenticationPrincipal Long userId) {
        int count = notificationService.getUnreadCount(userId);
        return Result.ok(Map.of("count", count));
    }

    /**
     * 标记单条通知为已读
     * @param id 通知ID
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 操作结果
     */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        notificationService.markRead(id, userId);
        return Result.ok();
    }

    /**
     * 标记所有通知为已读
     * @param userId 当前登录用户ID（从@AuthenticationPrincipal获取）
     * @return 操作结果
     */
    @PutMapping("/read-all")
    public Result<Void> markAllRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllRead(userId);
        return Result.ok();
    }
}
