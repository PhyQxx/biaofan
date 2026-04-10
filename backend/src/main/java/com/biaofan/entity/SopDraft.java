package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SOP草稿实体类
 * 存储用户执行SOP时的草稿数据，支持离线填写后同步
 */
@Data
@TableName("sop_draft")
public class SopDraft {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long executorId;
    private Long sopId;
    private String draftData;    // JSON: {stepId: {completed, note, photo}}
    private Long clientUpdatedAt;
    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
