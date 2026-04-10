package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.Notification;
import com.biaofan.mapper.NotificationMapper;
import com.biaofan.service.NotificationService;
import com.biaofan.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 站内通知服务实现类
 * 提供通知的查询、未读数统计、标记已读、创建通知等功能
 * 创建通知后自动通过NotificationDispatcher发送第三方推送（Webhook/邮件）
 *
 * @author biaofan
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationDispatcher notificationDispatcher;

    /**
     * 获取当前用户的消息通知列表
     * @param userId 用户ID
     * @return 通知列表，最多50条，按时间倒序
     */
    @Override
    public List<Notification> getMyNotifications(Long userId) {
        return notificationMapper.selectList(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt)
                .last("LIMIT 50")
        );
    }

    /**
     * 标记单条通知为已读
     * @param id 通知ID
     * @param userId 用户ID，校验归属
     */
    @Override
    public void markRead(Long id, Long userId) {
        notificationMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1)
        );
    }

    /**
     * 标记当前用户所有通知为已读
     * @param userId 用户ID
     */
    @Override
    public void markAllRead(Long userId) {
        notificationMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1)
        );
    }

    /**
     * 获取当前用户未读通知数量
     * @param userId 用户ID
     * @return 未读数
     */
    @Override
    public int getUnreadCount(Long userId) {
        return Math.toIntExact(notificationMapper.selectCount(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
        ));
    }

    /**
     * 创建新通知并发送
     * @param userId 接收用户ID
     * @param type 通知类型
     * @param title 通知标题
     * @param content 通知内容
     */
    @Override
    public void create(Long userId, String type, String title, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setIsRead(0);
        notificationMapper.insert(n);
        notificationDispatcher.dispatch(userId, title, content);
    }
}
