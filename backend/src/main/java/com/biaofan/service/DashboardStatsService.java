package com.biaofan.service;

import com.biaofan.dto.DashboardStatsDTO;
import java.util.List;

/**
 * 仪表盘统计服务接口
 * 提供SOP执行数据的统计和趋势分析功能
 */
public interface DashboardStatsService {
    /**
     * 获取用户仪表盘统计数据
     * 包含今日完成数、本周完成数、逾期数等汇总信息
     * @param userId 用户ID
     * @return 统计数据DTO
     */
    DashboardStatsDTO getDashboardStats(Long userId);

    /**
     * 获取执行趋势数据
     * @param days 统计天数
     * @return 趋势数据点列表
     */
    List<DashboardStatsDTO.TrendPoint> getTrend(int days);

    /**
     * 获取成员排行榜
     * @param limit 返回数量限制
     * @return 成员统计数据列表
     */
    List<DashboardStatsDTO.MemberStat> getLeaderboard(int limit);
}
