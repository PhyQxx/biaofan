package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

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
