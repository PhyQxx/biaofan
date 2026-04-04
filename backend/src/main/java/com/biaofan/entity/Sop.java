package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sop")
public class Sop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String content; // JSON string
    private String category;
    private String tags; // JSON array string
    private Integer version;
    private String status; // draft / published
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
