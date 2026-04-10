package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.ScheduleTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务Mapper接口
 *
 * <p>用于访问 schedule_task 表，提供定时任务配置的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface ScheduleTaskMapper extends BaseMapper<ScheduleTask> {

    /**
     * 查询已到期的定时任务
     *
     * <p>查询所有已启用且下次触发时间已到的任务，供调度器执行</p>
     *
     * @param now 当前时间，用于比较下次触发时间
     * @return 待执行的定时任务列表
     */
    @Select("SELECT * FROM schedule_task WHERE enabled = 1 AND next_fire_time <= #{now}")
    List<ScheduleTask> findDueTasks(@Param("now") LocalDateTime now);
}
