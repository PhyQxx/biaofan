package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("push_token")
public class PushToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String clientId;    // 个推 CID
    private String platform;    // android | ios
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
