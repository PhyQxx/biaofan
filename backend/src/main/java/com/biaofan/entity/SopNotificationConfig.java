package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sop_notification_config")
public class SopNotificationConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String platform;      // feishu / dingtalk
    private String webhookUrl;
    private String secret;
    private String botName;
    private Boolean enabled;      // true=启用 false=禁用
    private String triggerConfig; // JSON: which events to trigger
    private String email;
    private Long userId;          // null means global config
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
