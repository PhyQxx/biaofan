package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.SopInstance;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * SOP实例Mapper接口
 * 
 * 提供SOP实例数据的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * SOP实例表示某个SOP在特定时间或条件下生成的执行实例。
 */
@Mapper
public interface SopInstanceMapper extends BaseMapper<SopInstance> {
    
    /**
     * 插入实例（如果不存在唯一冲突则忽略）
     * 用于防止周期性实例重复创建
     */
    @Insert("INSERT IGNORE INTO sop_instance (sop_id, executor_id, period_start, period_end, status, current_step, created_at, updated_at) " +
            "VALUES (#{sopId}, #{executorId}, #{periodStart}, #{periodEnd}, #{status}, #{currentStep}, #{createdAt}, #{updatedAt})")
    int insertIgnore(SopInstance instance);
}
