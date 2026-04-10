package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.ExecutionStat;
import com.biaofan.entity.Sop;

/**
 * 执行统计服务实现
 * - 记录每条执行步骤的完成情况（ExecutionStepRecord）
 * - 聚合统计数据写入 ExecutionStat 表
 * - 提供 SOP 级别的执行完成率、异常率等指标
 */
import com.biaofan.entity.SopExecution;
import com.biaofan.mapper.ExecutionStatMapper;
import com.biaofan.mapper.SopExecutionMapper;
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
        return statMapper.selectList(
            new LambdaQueryWrapper<ExecutionStat>()
                .in(ExecutionStat::getSopId, sopIds)
                .orderByDesc(ExecutionStat::getLastExecutedAt)
        );
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
        return statMapper.selectList(
            new LambdaQueryWrapper<ExecutionStat>()
                .orderByDesc(ExecutionStat::getLastExecutedAt)
        );
    }

    /**
     * 确保SOP存在统计记录，如不存在则创建
     */
    private void ensureStat(Long sopId) {
        ExecutionStat stat = statMapper.selectOne(
            new LambdaQueryWrapper<ExecutionStat>().eq(ExecutionStat::getSopId, sopId)
        );
        if (stat == null) {
            List<SopExecution> execs = executionMapper.selectList(
                new LambdaQueryWrapper<SopExecution>()
                    .eq(SopExecution::getSopId, sopId)
                    .eq(SopExecution::getStatus, "completed")
            );
            ExecutionStat newStat = new ExecutionStat();
            newStat.setSopId(sopId);
            newStat.setTotalCount(execs.size());
            newStat.setCompletedCount(execs.size());
            newStat.setAvgDurationMinutes(0);
            statMapper.insert(newStat);
        }
    }
}
