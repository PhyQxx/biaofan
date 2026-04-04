package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.Notification;
import com.biaofan.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<List<Notification>> list(@AuthenticationPrincipal Long userId) {
        return Result.ok(notificationService.getMyNotifications(userId));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> unreadCount(@AuthenticationPrincipal Long userId) {
        int count = notificationService.getUnreadCount(userId);
        return Result.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        notificationService.markRead(id, userId);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllRead(userId);
        return Result.ok();
    }
}
