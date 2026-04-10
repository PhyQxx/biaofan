package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.GamificationUserBadge;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户徽章Mapper接口
 *
 * <p>用于访问 gamification_user_badge 表，提供用户已获得徽章记录的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface GamificationUserBadgeMapper extends BaseMapper<GamificationUserBadge> {
}
