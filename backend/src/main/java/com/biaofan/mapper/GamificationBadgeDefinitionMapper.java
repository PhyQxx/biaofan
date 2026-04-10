package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.GamificationBadgeDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 徽章定义Mapper接口
 *
 * <p>用于访问 gamification_badge_definition 表，提供徽章定义的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface GamificationBadgeDefinitionMapper extends BaseMapper<GamificationBadgeDefinition> {
}
