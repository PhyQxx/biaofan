package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.GamificationUserStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户积分统计Mapper接口
 *
 * <p>用于访问 gamification_user_stats 表，提供用户积分账户的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface GamificationUserStatsMapper extends BaseMapper<GamificationUserStats> {
    /**
     * 原子扣减积分
     *
     * <p>使用乐观锁机制原子扣减用户可用积分，防止并发环境下积分超额使用</p>
     *
     * @param userId 用户ID
     * @param price 本次扣减的积分数量
     * @return 影响行数，1表示扣减成功，0表示积分不足或用户不存在
     */
    // H-10: 原子扣减积分，防止并发超额使用
    @Update("UPDATE gamification_user_stats SET available_score = available_score - #{price} WHERE user_id = #{userId} AND available_score >= #{price}")
    int deductScore(@Param("userId") Long userId, @Param("price") int price);
}
