package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.ExecutionStat;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopExecution;
import com.biaofan.mapper.ExecutionStatMapper;
import com.biaofan.mapper.SopExecutionMapper;
import com.biaofan.mapper.SopMapper;
import com.biaofan.service.ExecutionStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExecutionStatServiceImpl implements ExecutionStatService {

    private final ExecutionStatMapper statMapper;
    private final SopMapper sopMapper;
    private final SopExecutionMapper executionMapper;

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
