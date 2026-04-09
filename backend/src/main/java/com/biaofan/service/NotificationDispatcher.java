package com.biaofan.service;

public interface NotificationDispatcher {
    void dispatch(Long userId, String title, String content);
}
