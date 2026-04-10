package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.SopNotificationConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * SOP通知配置Mapper接口
 * 
 * 提供SOP通知配置数据的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * 用于管理SOP任务的通知提醒配置。
 */
@Mapper
public interface SopNotificationConfigMapper extends BaseMapper<SopNotificationConfig> {
}
