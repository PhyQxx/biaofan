package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知消息Mapper接口
 * 
 * 提供通知消息数据的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * 用于管理系统和用户之间的通知消息。
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
