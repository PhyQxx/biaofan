package com.biaofan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("execution_stat")
public class ExecutionStat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sopId;
    private Integer totalCount;
    private Integer completedCount;
    private Integer avgDurationMinutes;
    private LocalDateTime lastExecutedAt;
}
