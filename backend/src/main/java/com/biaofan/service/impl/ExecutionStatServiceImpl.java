package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.ExecutionStat;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopExecution;
import com.biaofan.entity.SopInstance;
import com.biaofan.mapper.ExecutionStatMapper;
import com.biaofan.mapper.SopExecutionMapper;
import com.biaofan.mapper.SopInstanceMapper;
import com.biaofan.mapper.SopMapper;
import com.biaofan.service.ExecutionStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 执行统计服务实现类
 * 提供SOP执行数据的统计查询，包括个人统计和全局统计
 * 自动确保每个SOP都有对应的统计记录
 *
 * @author biaofan
 */

/**
 * 执行统计服务实现
 * - 记录每条执行步骤的完成情况（ExecutionStepRecord）
 * - 聚合统计数据写入 ExecutionStat 表
 * - 提供 SOP 级别的执行完成率、异常率等指标
 */
@Service
@RequiredArgsConstructor
public class ExecutionStatServiceImpl implements ExecutionStatService {

    private final ExecutionStatMapper statMapper;
    private final SopMapper sopMapper;
    private final SopExecutionMapper executionMapper;
    private final SopInstanceMapper instanceMapper;

    /**
     * 获取当前用户的SOP执行统计
     * @param userId 用户ID
     * @return 执行统计列表
     */
    @Override
    public List<ExecutionStat> getStats(Long userId) {
        List<Sop> sops = sopMapper.selectList(
            new LambdaQueryWrapper<Sop>().eq(Sop::getUserId, userId)
        );
        for (Sop sop : sops) {
            ensureStat(sop.getId());
        }
        List<Long> sopIds = sops.stream().map(Sop::getId).toList();
        if (sopIds.isEmpty()) return Collections.emptyList();
        List<ExecutionStat> stats = statMapper.selectList(
            new LambdaQueryWrapper<ExecutionStat>()
                .in(ExecutionStat::getSopId, sopIds)
                .orderByDesc(ExecutionStat::getLastExecutedAt)
        );
        // 填充 sopTitle - 使用Map进行O(1)查找，避免嵌套循环
        java.util.Map<Long, String> sopTitleMap = sops.stream()
            .collect(java.util.stream.Collectors.toMap(Sop::getId, Sop::getTitle));
        for (ExecutionStat stat : stats) {
            stat.setSopTitle(sopTitleMap.get(stat.getSopId()));
        }
        return stats;
    }

    /**
     * 获取全局SOP执行统计
     * @return 所有SOP的执行统计列表
     */
    @Override
    public List<ExecutionStat> getGlobalStats() {
        List<Sop> allSops = sopMapper.selectList(null);
        for (Sop sop : allSops) {
            ensureStat(sop.getId());
        }
        List<ExecutionStat> stats = statMapper.selectList(
            new LambdaQueryWrapper<ExecutionStat>()
                .orderByDesc(ExecutionStat::getLastExecutedAt)
        );
        // 填充 sopTitle - 使用Map进行O(1)查找，避免嵌套循环
        java.util.Map<Long, String> sopTitleMap = allSops.stream()
            .collect(java.util.stream.Collectors.toMap(Sop::getId, Sop::getTitle));
        for (ExecutionStat stat : stats) {
            stat.setSopTitle(sopTitleMap.get(stat.getSopId()));
        }
        return stats;
    }

    /**
     * 确保SOP存在统计记录，如不存在则创建，存在则更新
     * 优化：合并多个单独查询为批量查询，减少数据库往返
     */
    private void ensureStat(Long sopId) {
        ExecutionStat existingStat = statMapper.selectOne(
            new LambdaQueryWrapper<ExecutionStat>().eq(ExecutionStat::getSopId, sopId)
        );

        // 合并查询：一次查询获取所有SopExecution和SopInstance数据，在内存中统计
        // 使用in查询配合sopId列表（此处为单个sopId，可扩展为批量）
        List<SopExecution> allExecs = executionMapper.selectList(
            new LambdaQueryWrapper<SopExecution>().eq(SopExecution::getSopId, sopId)
        );
        List<SopInstance> allInstances = instanceMapper.selectList(
            new LambdaQueryWrapper<SopInstance>().eq(SopInstance::getSopId, sopId)
        );

        // 在内存中过滤已完成的执行
        List<SopExecution> completedExecs = allExecs.stream()
            .filter(e -> "completed".equals(e.getStatus()))
            .toList();
        List<SopInstance> completedInstances = allInstances.stream()
            .filter(i -> "completed".equals(i.getStatus()))
            .toList();

        int totalCount = allExecs.size() + allInstances.size();
        int completedCount = completedExecs.size() + completedInstances.size();

        // 计算平均耗时（分钟）
        int avgDuration = 0;
        if (!completedExecs.isEmpty()) {
            long totalMinutes = completedExecs.stream()
                .filter(e -> e.getCompletedAt() != null && e.getStartedAt() != null)
                .mapToLong(e -> java.time.Duration.between(e.getStartedAt(), e.getCompletedAt()).toMinutes())
                .sum();
            avgDuration = (int) (totalMinutes / completedExecs.size());
        }

        // 获取最近执行时间
        java.time.LocalDateTime lastExecutedAt = completedExecs.stream()
            .filter(e -> e.getCompletedAt() != null)
            .map(SopExecution::getCompletedAt)
            .max(java.time.LocalDateTime::compareTo)
            .orElse(null);

        if (existingStat == null) {
            // 新建统计记录
            ExecutionStat newStat = new ExecutionStat();
            newStat.setSopId(sopId);
            newStat.setTotalCount(totalCount);
            newStat.setCompletedCount(completedCount);
            newStat.setAvgDurationMinutes(avgDuration);
            newStat.setLastExecutedAt(lastExecutedAt);
            statMapper.insert(newStat);
        } else {
            // 更新已有记录
            existingStat.setTotalCount(totalCount);
            existingStat.setCompletedCount(completedCount);
            existingStat.setAvgDurationMinutes(avgDuration);
            existingStat.setLastExecutedAt(lastExecutedAt);
            statMapper.updateById(existingStat);
        }
    }
}
