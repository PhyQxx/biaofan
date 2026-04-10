package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.SopInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * SOP实例Mapper接口
 * 
 * 提供SOP实例数据的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * SOP实例表示某个SOP在特定时间或条件下生成的执行实例。
 */
@Mapper
public interface SopInstanceMapper extends BaseMapper<SopInstance> {
}
