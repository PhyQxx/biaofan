package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.ExecutionStat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 执行统计Mapper接口
 * 
 * 提供执行统计数据的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * 用于记录和查询SOP执行相关的统计数据。
 */
@Mapper
public interface ExecutionStatMapper extends BaseMapper<ExecutionStat> {
}
