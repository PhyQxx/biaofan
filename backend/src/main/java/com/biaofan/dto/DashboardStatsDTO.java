package com.biaofan.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStatsDTO {
    private int todayTotal;          // 今日待执行总数
    private int todayCompleted;      // 今日已完成
    private int todayOverdue;        // 今日超时
    private int pendingCount;        // 待执行总数（进行中）
    private int completedCount;     // 已完成总数
    private double completionRate;    // 完成率百分比
    private int totalMembers;        // 参与成员数
    private int totalSOPs;          // 活跃SOP数

    // 近7日趋势数据
    private List<TrendPoint> trend;

    // TOP执行人排名
    private List<MemberStat> topMembers;

    @Data
    public static class TrendPoint {
        private String date;
        private int total;
        private int completed;
        private double rate;
    }

    @Data
    public static class MemberStat {
        private Long userId;
        private String username;
        private int completedCount;
        private int overdueCount;
        private double completionRate;
        private int rank;
    }
}
