package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

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
