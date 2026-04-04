package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.ScheduleTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ScheduleTaskMapper extends BaseMapper<ScheduleTask> {

    @Select("SELECT * FROM schedule_task WHERE enabled = 1 AND next_fire_time <= #{now}")
    List<ScheduleTask> findDueTasks(@Param("now") LocalDateTime now);
}
