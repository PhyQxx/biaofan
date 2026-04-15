package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮件服务配置实体类
 * 全局单条记录，管理后台可编辑
 */
@Data
@TableName("email_config")
public class EmailConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** SMTP 服务器地址 */
    private String host;
    /** SMTP 端口 */
    private Integer port;
    /** 发件邮箱账号 */
    private String username;
    /** 授权码/密码 */
    private String password;
    /** 发件人地址（默认同 username） */
    private String fromAddress;
    /** 是否启用 SMTP 认证 */
    private Boolean smtpAuth;
    /** 是否启用 STARTTLS */
    private Boolean starttlsEnable;
    /** 是否启用邮件服务 */
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
