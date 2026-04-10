package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.GamificationUserStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GamificationUserStatsMapper extends BaseMapper<GamificationUserStats> {
    // H-10: 原子扣减积分，防止并发超额使用
    @Update("UPDATE gamification_user_stats SET available_score = available_score - #{price} WHERE user_id = #{userId} AND available_score >= #{price}")
    int deductScore(@Param("userId") Long userId, @Param("price") int price);
}
