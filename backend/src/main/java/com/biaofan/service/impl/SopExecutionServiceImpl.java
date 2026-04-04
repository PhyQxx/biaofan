package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.*;
import com.biaofan.mapper.*;
import com.biaofan.service.SopExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SopExecutionServiceImpl implements SopExecutionService {

    private final SopExecutionMapper executionMapper;
    private final SopMapper sopMapper;
    private final ExecutionStepRecordMapper stepRecordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public SopExecution startExecution(Long userId, Long sopId) {
        Sop sop = sopMapper.selectById(sopId);
        if (sop == null) throw new RuntimeException("SOP不存在");
        if (!"published".equals(sop.getStatus())) throw new RuntimeException("SOP未发布");

        SopExecution ongoing = executionMapper.selectOne(
            new LambdaQueryWrapper<SopExecution>()
                .eq(SopExecution::getExecutorId, userId)
                .eq(SopExecution::getSopId, sopId)
                .in(SopExecution::getStatus, "pending", "in_progress")
        );
        if (ongoing != null) return ongoing;

        SopExecution e = new SopExecution();
        e.setSopId(sopId);
        e.setSopVersion(sop.getVersion());
        e.setExecutorId(userId);
        e.setStatus("in_progress");
        e.setCurrentStep(1);
        e.setStartedAt(LocalDateTime.now());
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        executionMapper.insert(e);
        return e;
    }

    @Override
    @Transactional
    public boolean completeStep(Long userId, Long executionId, int stepIndex, String notes,
                                 Map<String, Object> checkData, String attachments) {
        SopExecution e = getExecution(executionId);
        if (!e.getExecutorId().equals(userId)) throw new RuntimeException("无权操作");
        if (!"in_progress".equals(e.getStatus())) throw new RuntimeException("执行状态不允许操作");

        Sop sop = sopMapper.selectById(e.getSopId());
        List<?> steps = parseJson(sop.getContent(), List.class);
        if (steps == null) steps = Collections.emptyList();

        ExecutionStepRecord record = new ExecutionStepRecord();
        record.setExecutionId(executionId);
        record.setStepIndex(stepIndex);

        if (steps.size() >= stepIndex) {
            Map<?, ?> step = (Map<?, ?>) steps.get(stepIndex - 1);
            record.setStepTitle((String) step.get("title"));
        }
        record.setCompletedAt(LocalDateTime.now());
        record.setNotes(notes);
        if (checkData != null) {
            try {
                record.setCheckData(objectMapper.writeValueAsString(checkData));
            } catch (Exception ex) { record.setCheckData("{}"); }
        }
        record.setAttachments(attachments);
        stepRecordMapper.insert(record);

        boolean completed = stepIndex >= steps.size();
        if (completed) {
            e.setStatus("completed");
            e.setCompletedAt(LocalDateTime.now());
            e.setCurrentStep(stepIndex);
        } else {
            e.setCurrentStep(stepIndex + 1);
        }
        e.setUpdatedAt(LocalDateTime.now());
        executionMapper.updateById(e);
        return completed;
    }

    @Override
    @Transactional
    public void finishExecution(Long userId, Long executionId) {
        SopExecution e = getExecution(executionId);
        if (!e.getExecutorId().equals(userId)) throw new RuntimeException("无权操作");
        e.setStatus("completed");
        e.setCompletedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        executionMapper.updateById(e);
    }

    @Override
    public List<SopExecution> getMyExecutions(Long userId, String status) {
        LambdaQueryWrapper<SopExecution> q = new LambdaQueryWrapper<SopExecution>()
                .eq(SopExecution::getExecutorId, userId)
                .orderByDesc(SopExecution::getCreatedAt);
        if (status != null && !status.isEmpty()) {
            q.eq(SopExecution::getStatus, status);
        }
        return executionMapper.selectList(q);
    }

    @Override
    public SopExecution getExecution(Long executionId) {
        SopExecution e = executionMapper.selectById(executionId);
        if (e == null) throw new RuntimeException("执行记录不存在");
        return e;
    }

    @Override
    public Sop getSopWithSteps(Long sopId) {
        return sopMapper.selectById(sopId);
    }

    @Override
    public List<ExecutionStepRecord> getStepRecords(Long executionId) {
        return stepRecordMapper.selectList(
            new LambdaQueryWrapper<ExecutionStepRecord>()
                .eq(ExecutionStepRecord::getExecutionId, executionId)
                .orderByAsc(ExecutionStepRecord::getStepIndex)
        );
    }

    @Override
    public int getStepCount(Long executionId) {
        SopExecution e = getExecution(executionId);
        Sop sop = sopMapper.selectById(e.getSopId());
        List<?> steps = parseJson(sop.getContent(), List.class);
        return steps != null ? steps.size() : 0;
    }

    private <T> T parseJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }
}
