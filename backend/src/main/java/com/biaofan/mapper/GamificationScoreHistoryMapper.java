package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.GamificationScoreHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分历史Mapper接口
 *
 * <p>用于访问 gamification_score_history 表，提供用户积分变动历史记录的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface GamificationScoreHistoryMapper extends BaseMapper<GamificationScoreHistory> {
}
