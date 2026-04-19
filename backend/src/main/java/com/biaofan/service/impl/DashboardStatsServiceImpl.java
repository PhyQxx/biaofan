package com.biaofan.service.impl;


/**
 * 仪表盘统计服务实现
 * - 汇总用户总数、SOP总数、今日执行次数、异常数等
 * - 提供管理后台首页数据支撑
 */
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

/**
 * 仪表盘统计服务实现类
 * 提供今日/总体执行数据、趋势图、排行榜等Dashboard数据
 * 统计维度包括完成率、逾期数、活跃成员等
 *
 * @author biaofan
 */
@Service
@RequiredArgsConstructor
public class DashboardStatsServiceImpl implements DashboardStatsService {

    private final SopExecutionMapper executionMapper;
    private final SopDispatchMapper dispatchMapper;
    private final SopMapper sopMapper;
    private final UserMapper userMapper;

    /**
     * 获取仪表盘统计数据
     * @param userId 用户ID（可选，传null为全局统计）
     * @return 包含今日数据、总体数据、趋势、排行榜等
     */
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

    /**
     * 获取执行完成率趋势
     * @param days 统计天数
     * @return 每日趋势数据点列表
     */
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

    /**
     * 获取成员排行榜
     * @param limit 返回数量
     * @return 成员统计数据列表
     */
    @Override
    public List<DashboardStatsDTO.MemberStat> getLeaderboard(int limit) {
        List<Map<String, Object>> raw = executionMapper.getMemberLeaderboard();
        List<DashboardStatsDTO.MemberStat> result = new ArrayList<>();

        if (!raw.isEmpty()) {
            // 批量获取用户信息，一次查询替代循环内N次查询
            List<Long> userIds = raw.stream()
                .map(row -> ((Number) row.get("executor_id")).longValue())
                .toList();
            java.util.Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

            int rank = 1;
            for (Map<String, Object> row : raw) {
                DashboardStatsDTO.MemberStat s = new DashboardStatsDTO.MemberStat();
                Long userId = ((Number) row.get("executor_id")).longValue();
                s.setUserId(userId);
                s.setCompletedCount(((Number) row.get("completed_count")).intValue());
                s.setOverdueCount(((Number) row.get("overdue_count")).intValue());
                s.setRank(rank++);
                // 从预加载的Map中获取用户名
                User user = userMap.get(userId);
                s.setUsername(user != null ? user.getUsername() : "用户" + userId);
                int total = s.getCompletedCount() + s.getOverdueCount();
                s.setCompletionRate(total > 0
                    ? Math.round((double) s.getCompletedCount() / total * 1000) / 10.0 : 0.0);
                result.add(s);
            }
        }
        return result;
    }
}
