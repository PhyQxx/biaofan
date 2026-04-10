package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SOP版本实体类
 * 记录SOP的版本历史，每次发布新版本时创建快照，用于版本管理和回溯
 */
@Data
@TableName("sop_version")
public class SopVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sopId;
    private Integer version;
    private String changeSummary;
    private String content; // JSON snapshot
    private Integer isCurrent; // 0=历史 1=当前
    private Long creatorId;
    private LocalDateTime createdAt;
}
