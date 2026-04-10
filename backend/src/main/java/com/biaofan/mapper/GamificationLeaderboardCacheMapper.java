package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.GamificationLeaderboardCache;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排行榜缓存Mapper接口
 *
 * <p>用于访问 gamification_leaderboard_cache 表，提供排行榜缓存数据的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface GamificationLeaderboardCacheMapper extends BaseMapper<GamificationLeaderboardCache> {
}
