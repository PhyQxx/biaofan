package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.ExecutionStepRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 执行步骤记录Mapper接口
 * 
 * 提供执行步骤记录数据的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * 记录SOP执行过程中每个步骤的完成情况。
 */
@Mapper
public interface ExecutionStepRecordMapper extends BaseMapper<ExecutionStepRecord> {
}
