package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 * 
 * 提供用户数据的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * 可使用BaseMapper提供的标准CRUD方法，以及自定义的查询方法。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
