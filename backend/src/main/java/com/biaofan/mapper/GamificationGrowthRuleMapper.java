package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.GamificationGrowthRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成长规则Mapper接口
 *
 * <p>用于访问 gamification_growth_rule 表，提供成长规则的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface GamificationGrowthRuleMapper extends BaseMapper<GamificationGrowthRule> {
}
