package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sop_dispatch")
public class SopDispatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;  // SOP模板ID
    private String templateIds; // JSON array of SOP IDs (批量分发)
    private String assigneeIds; // JSON array of user IDs
    private Long dispatcherId;   // 分发人ID
    private String status;     // pending / distributed / completed
    private LocalDateTime dispatchedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
