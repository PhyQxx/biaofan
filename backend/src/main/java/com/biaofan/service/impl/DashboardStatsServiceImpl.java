package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.dto.DashboardStatsDTO;
import com.biaofan.entity.SopDispatch;
import com.biaofan.entity.SopExecution;
import com.biaofan.entity.User;
import com.biaofan.mapper.SopDispatchMapper;
import com.biaofan.mapper.SopExecutionMapper;
import com.biaofan.mapper.SopMapper;
import com.biaofan.mapper.UserMapper;
import com.biaofan.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardStatsServiceImpl implements DashboardStatsService {

    private final SopExecutionMapper executionMapper;
    private final SopDispatchMapper dispatchMapper;
    private final SopMapper sopMapper;
    private final UserMapper userMapper;

    @Override
    public DashboardStatsDTO getDashboardStats(Long userId) {
        DashboardStatsDTO dto = new DashboardStatsDTO();

        // Today's stats
        dto.setTodayTotal(executionMapper.countTodayTotal());
        dto.setTodayCompleted(executionMapper.countTodayCompleted());
        dto.setTodayOverdue(executionMapper.countTodayOverdue());

        // Overall pending
        long pending = executionMapper.selectCount(
            new LambdaQueryWrapper<SopExecution>().eq(SopExecution::getStatus, "pending"));
        dto.setPendingCount((int) pending);

        // Overall completed
        long completed = executionMapper.selectCount(
            new LambdaQueryWrapper<SopExecution>().eq(SopExecution::getStatus, "completed"));
        dto.setCompletedCount((int) completed);

        // Completion rate
        long total = executionMapper.selectCount(null);
        if (total > 0) {
            dto.setCompletionRate(Math.round((double) completed / total * 1000) / 10.0);
        } else {
            dto.setCompletionRate(0.0);
        }

        // Active members
        dto.setTotalMembers(executionMapper.countActiveMembers());

        // Active SOPs (with dispatches)
        long activeSops = sopMapper.selectCount(null);
        dto.setTotalSOPs((int) activeSops);

        // Trend (last 7 days)
        dto.setTrend(getTrend(7));

        // Top members
        dto.setTopMembers(getLeaderboard(5));

        return dto;
    }

    @Override
    public List<DashboardStatsDTO.TrendPoint> getTrend(int days) {
        List<Map<String, Object>> raw = executionMapper.getCompletionTrend(days);
        Map<String, DashboardStatsDTO.TrendPoint> pointMap = new LinkedHashMap<>();

        // Initialize all dates with 0
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            DashboardStatsDTO.TrendPoint p = new DashboardStatsDTO.TrendPoint();
            p.setDate(d.format(fmt));
            p.setTotal(0);
            p.setCompleted(0);
            p.setRate(0.0);
            pointMap.put(d.format(fmt), p);
        }

        // Fill in actual data
        for (Map<String, Object> row : raw) {
            String dateStr = row.get("date").toString();
            if (pointMap.containsKey(dateStr)) {
                DashboardStatsDTO.TrendPoint p = pointMap.get(dateStr);
                int total = ((Number) row.get("total")).intValue();
                int completed = ((Number) row.get("completed")).intValue();
                p.setTotal(total);
                p.setCompleted(completed);
                p.setRate(total > 0 ? Math.round((double) completed / total * 1000) / 10.0 : 0.0);
            }
        }

        return new ArrayList<>(pointMap.values());
    }

    @Override
    public List<DashboardStatsDTO.MemberStat> getLeaderboard(int limit) {
        List<Map<String, Object>> raw = executionMapper.getMemberLeaderboard();
        List<DashboardStatsDTO.MemberStat> result = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : raw) {
            DashboardStatsDTO.MemberStat s = new DashboardStatsDTO.MemberStat();
            Long userId = ((Number) row.get("executor_id")).longValue();
            s.setUserId(userId);
            s.setCompletedCount(((Number) row.get("completed_count")).intValue());
            s.setOverdueCount(((Number) row.get("overdue_count")).intValue());
            s.setRank(rank++);
            // Fetch username
            User user = userMapper.selectById(userId);
            s.setUsername(user != null ? user.getUsername() : "用户" + userId);
            int total = s.getCompletedCount() + s.getOverdueCount();
            s.setCompletionRate(total > 0
                ? Math.round((double) s.getCompletedCount() / total * 1000) / 10.0 : 0.0);
            result.add(s);
        }
        return result;
    }
}
