package com.biaofan.service;

/**
 * 通知分发器 — 根据用户配置的平台（系统通知/飞书webhook/邮箱）发送通知
 */
public interface NotificationDispatcher {

    /**
     * 向指定用户发送通知
     *
     * @param userId  用户ID
     * @param title   通知标题
     * @param content 通知内容
     */
    void dispatch(Long userId, String title, String content);
}
