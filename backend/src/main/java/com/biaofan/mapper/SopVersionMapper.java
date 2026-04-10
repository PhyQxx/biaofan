package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.SopVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * SOP版本Mapper接口
 * 
 * 提供SOP版本数据的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * SOP版本用于记录SOP标准操作规程的版本变更历史。
 */
@Mapper
public interface SopVersionMapper extends BaseMapper<SopVersion> {
}
