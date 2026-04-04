package com.biaofan.service;

import com.biaofan.entity.Notification;
import java.util.List;

public interface NotificationService {
    List<Notification> getMyNotifications(Long userId);
    void markRead(Long id, Long userId);
    void markAllRead(Long userId);
    int getUnreadCount(Long userId);
    void create(Long userId, String type, String title, String content);
}
