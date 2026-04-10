package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知消息实体类
 * 存储系统向用户发送的通知消息，包括执行提醒、到期通知等
 */
@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;      // execution_reminder / execution_overdue / sop_published / version_updated
    private String title;
    private String content;
    private String sourceType; // execution / sop / version
    private Long sourceId;
    private Integer isRead;    // 0=未读 1=已读
    private LocalDateTime readTime;
    private LocalDateTime createdAt;
}
