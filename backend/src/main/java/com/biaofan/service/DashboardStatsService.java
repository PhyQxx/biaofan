package com.biaofan.service;

import com.biaofan.dto.DashboardStatsDTO;
import java.util.List;

public interface DashboardStatsService {
    DashboardStatsDTO getDashboardStats(Long userId);
    List<DashboardStatsDTO.TrendPoint> getTrend(int days);
    List<DashboardStatsDTO.MemberStat> getLeaderboard(int limit);
}
