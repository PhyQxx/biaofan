package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopDispatch;
import com.biaofan.entity.SopExecution;
import com.biaofan.mapper.SopDispatchMapper;
import com.biaofan.mapper.SopExecutionMapper;
import com.biaofan.mapper.SopMapper;
import com.biaofan.service.SopDispatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SopDispatchServiceImpl implements SopDispatchService {

    private final SopDispatchMapper dispatchMapper;
    private final SopMapper sopMapper;
    private final SopExecutionMapper executionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public List<SopDispatch> batchDispatch(Long dispatcherId, List<Long> templateIds, List<Long> assigneeIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            throw new RuntimeException("模板ID列表不能为空");
        }
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            throw new RuntimeException("指派人ID列表不能为空");
        }

        List<SopDispatch> results = new ArrayList<>();

        for (Long sopId : templateIds) {
            Sop sop = sopMapper.selectById(sopId);
            if (sop == null) continue;
            if (!"published".equals(sop.getStatus())) continue;

            for (Long assigneeId : assigneeIds) {
                // 创建执行记录
                SopExecution exec = new SopExecution();
                exec.setSopId(sopId);
                exec.setSopVersion(sop.getVersion());
                exec.setExecutorId(assigneeId);
                exec.setStatus("pending");
                exec.setCurrentStep(1);
                exec.setCreatedAt(LocalDateTime.now());
                exec.setUpdatedAt(LocalDateTime.now());
                executionMapper.insert(exec);

                // 创建分发记录
                SopDispatch dispatch = new SopDispatch();
                dispatch.setTemplateId(sopId);
                dispatch.setDispatcherId(dispatcherId);
                dispatch.setStatus("distributed");
                dispatch.setDispatchedAt(LocalDateTime.now());
                dispatch.setCreatedAt(LocalDateTime.now());
                dispatch.setUpdatedAt(LocalDateTime.now());
                try {
                    dispatch.setTemplateIds(objectMapper.writeValueAsString(templateIds));
                    dispatch.setAssigneeIds(objectMapper.writeValueAsString(assigneeIds));
                } catch (Exception e) { /* ignore */ }
                dispatchMapper.insert(dispatch);
                results.add(dispatch);
            }
        }
        return results;
    }

    @Override
    public List<SopDispatch> getMyDispatches(Long userId) {
        return dispatchMapper.selectList(
            new LambdaQueryWrapper<SopDispatch>()
                .eq(SopDispatch::getDispatcherId, userId)
                .orderByDesc(SopDispatch::getCreatedAt)
        );
    }
}
