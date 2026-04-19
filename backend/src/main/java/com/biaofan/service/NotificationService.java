package com.biaofan.service;

import com.biaofan.entity.Notification;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;

/**
 * 系统通知服务接口
 * 提供通知的查询、标记已读、创建等功能
 */
public interface NotificationService {
    /**
     * 获取当前用户的通知列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 通知分页列表（按创建时间倒序）
     */
    Page<Notification> getMyNotifications(Long userId, int page, int pageSize);

    /**
     * 标记单条通知为已读
     * @param id 通知ID
     * @param userId 当前用户ID（用于校验归属）
     */
    void markRead(Long id, Long userId);

    /**
     * 标记当前用户所有通知为已读
     * @param userId 用户ID
     */
    void markAllRead(Long userId);

    /**
     * 获取当前用户未读通知数量
     * @param userId 用户ID
     * @return 未读数量
     */
    int getUnreadCount(Long userId);

    /**
     * 创建一条通知
     * @param userId 通知所属用户ID
     * @param type 通知类型
     * @param title 通知标题
     * @param content 通知内容
     */
    void create(Long userId, String type, String title, String content);
}
