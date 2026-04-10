package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SOP通知配置实体类
 * 配置SOP相关事件的第三方通知推送，支持飞书、钉钉等平台
 */
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
