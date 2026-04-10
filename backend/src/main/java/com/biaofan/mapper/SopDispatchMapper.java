package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.SopDispatch;
import org.apache.ibatis.annotations.Mapper;

/**
 * SOP调度Mapper接口
 * 
 * 提供SOP调度数据的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * SOP调度用于管理SOP任务的触发和调度规则。
 */
@Mapper
public interface SopDispatchMapper extends BaseMapper<SopDispatch> {
}
