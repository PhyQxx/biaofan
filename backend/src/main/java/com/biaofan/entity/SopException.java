package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SOP异常记录实体类
 * 记录SOP执行过程中出现的异常情况，包括异常类型、描述和处理状态
 */
@Data
@TableName("sop_exception")
public class SopException {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long executionId;   // 执行记录ID
    private Long reporterId;   // 上报人
    private String type;       // abnormal / timeout / error
    private String description; // 异常描述
    private String status;     // pending / resolved
    private Long resolvedBy;   // 处理人
    private String resolution; // 处理说明
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
