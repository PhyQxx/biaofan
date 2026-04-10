package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.SopException;
import org.apache.ibatis.annotations.Mapper;

/**
 * SOP异常Mapper接口
 * 
 * 提供SOP异常数据的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * 记录SOP执行过程中出现的异常或错误信息。
 */
@Mapper
public interface SopExceptionMapper extends BaseMapper<SopException> {
}
