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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 执行统计服务实现类
 * 提供SOP执行数据的统计查询，包括个人统计和全局统计
 * 使用批量查询替代逐条查询，减少数据库往返次数
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
        if (sops.isEmpty()) return Collections.emptyList();

        List<Long> sopIds = sops.stream().map(Sop::getId).toList();
        Map<Long, String> sopTitleMap = sops.stream()
            .collect(Collectors.toMap(Sop::getId, Sop::getTitle));

        // Batch fetch existing stats
        Map<Long, ExecutionStat> existingStats = statMapper.selectList(
            new LambdaQueryWrapper<ExecutionStat>().in(ExecutionStat::getSopId, sopIds)
        ).stream().collect(Collectors.toMap(ExecutionStat::getSopId, s -> s));

        // Batch fetch all executions for these SOPs
        Map<Long, List<SopExecution>> execsBySop = executionMapper.selectList(
            new LambdaQueryWrapper<SopExecution>().in(SopExecution::getSopId, sopIds)
        ).stream().collect(Collectors.groupingBy(SopExecution::getSopId));

        // Batch fetch all instances for these SOPs
        Map<Long, List<SopInstance>> instancesBySop = instanceMapper.selectList(
            new LambdaQueryWrapper<SopInstance>().in(SopInstance::getSopId, sopIds)
        ).stream().collect(Collectors.groupingBy(SopInstance::getSopId));

        // Compute stats in memory
        for (Long sopId : sopIds) {
            computeAndSaveStat(sopId, existingStats.get(sopId),
                execsBySop.getOrDefault(sopId, Collections.emptyList()),
                instancesBySop.getOrDefault(sopId, Collections.emptyList()));
        }

        List<ExecutionStat> stats = statMapper.selectList(
            new LambdaQueryWrapper<ExecutionStat>()
                .in(ExecutionStat::getSopId, sopIds)
                .orderByDesc(ExecutionStat::getLastExecutedAt));
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
        List<Sop> allSops = sopMapper.selectList(
            new LambdaQueryWrapper<Sop>().last("LIMIT 10000"));
        if (allSops.isEmpty()) return Collections.emptyList();

        List<Long> sopIds = allSops.stream().map(Sop::getId).toList();
        Map<Long, String> sopTitleMap = allSops.stream()
            .collect(Collectors.toMap(Sop::getId, Sop::getTitle));

        // Batch fetch existing stats
        Map<Long, ExecutionStat> existingStats = statMapper.selectList(
            new LambdaQueryWrapper<ExecutionStat>().in(ExecutionStat::getSopId, sopIds)
        ).stream().collect(Collectors.toMap(ExecutionStat::getSopId, s -> s));

        // Batch fetch all executions for these SOPs
        Map<Long, List<SopExecution>> execsBySop = executionMapper.selectList(
            new LambdaQueryWrapper<SopExecution>().in(SopExecution::getSopId, sopIds)
        ).stream().collect(Collectors.groupingBy(SopExecution::getSopId));

        // Batch fetch all instances for these SOPs
        Map<Long, List<SopInstance>> instancesBySop = instanceMapper.selectList(
            new LambdaQueryWrapper<SopInstance>().in(SopInstance::getSopId, sopIds)
        ).stream().collect(Collectors.groupingBy(SopInstance::getSopId));

        // Compute stats in memory
        for (Long sopId : sopIds) {
            computeAndSaveStat(sopId, existingStats.get(sopId),
                execsBySop.getOrDefault(sopId, Collections.emptyList()),
                instancesBySop.getOrDefault(sopId, Collections.emptyList()));
        }

        List<ExecutionStat> stats = statMapper.selectList(
            new LambdaQueryWrapper<ExecutionStat>()
                .in(ExecutionStat::getSopId, sopIds)
                .orderByDesc(ExecutionStat::getLastExecutedAt));
        for (ExecutionStat stat : stats) {
            stat.setSopTitle(sopTitleMap.get(stat.getSopId()));
        }
        return stats;
    }

    /**
     * 根据预取的数据计算并保存单个SOP的统计记录
     * 接收批量查询的预取列表，避免N+1查询
     *
     * @param sopId SOP ID
     * @param existingStat 已存在的统计记录（可能为null）
     * @param allExecs 该SOP的所有执行记录
     * @param allInstances 该SOP的所有实例记录
     */
    private void computeAndSaveStat(Long sopId, ExecutionStat existingStat,
                                     List<SopExecution> allExecs, List<SopInstance> allInstances) {
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
            ExecutionStat newStat = new ExecutionStat();
            newStat.setSopId(sopId);
            newStat.setTotalCount(totalCount);
            newStat.setCompletedCount(completedCount);
            newStat.setAvgDurationMinutes(avgDuration);
            newStat.setLastExecutedAt(lastExecutedAt);
            statMapper.insert(newStat);
        } else {
            existingStat.setTotalCount(totalCount);
            existingStat.setCompletedCount(completedCount);
            existingStat.setAvgDurationMinutes(avgDuration);
            existingStat.setLastExecutedAt(lastExecutedAt);
            statMapper.updateById(existingStat);
        }
    }
}
