package com.biaofan.service;

/**
 * 通知分发器接口
 * 根据用户配置的渠道（系统通知/飞书Webhook/邮箱等）发送通知
 */
public interface NotificationDispatcher {

    /**
     * 向指定用户发送通知
     * 根据用户的通知配置，自动选择合适的渠道发送
     * @param userId  用户ID
     * @param title   通知标题
     * @param content 通知内容
     */
    void dispatch(Long userId, String title, String content);
}
