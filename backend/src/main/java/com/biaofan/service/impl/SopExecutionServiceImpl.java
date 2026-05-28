package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.*;
import com.biaofan.mapper.*;
import com.biaofan.service.GamificationService;
import com.biaofan.service.NotificationService;
import com.biaofan.service.SopExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * SOP执行服务实现类
 */
@Slf4j
@Service
public class SopExecutionServiceImpl implements SopExecutionService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final SopExecutionMapper executionMapper;
    private final SopMapper sopMapper;
    private final ExecutionStepRecordMapper stepRecordMapper;
    private final NotificationService notificationService;
    private final GamificationService gamificationService;
    private final ObjectMapper objectMapper;

    public SopExecutionServiceImpl(
            SopExecutionMapper executionMapper,
            SopMapper sopMapper,
            ExecutionStepRecordMapper stepRecordMapper,
            NotificationService notificationService,
            GamificationService gamificationService,
            ObjectMapper objectMapper) {
        this.executionMapper = executionMapper;
        this.sopMapper = sopMapper;
        this.stepRecordMapper = stepRecordMapper;
        this.notificationService = notificationService;
        this.gamificationService = gamificationService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SopExecution startExecution(Long userId, Long sopId) {
        String idempotencyKey = "exec_start:" + userId + ":" + sopId;
        Boolean started = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(started)) {
            SopExecution existing = executionMapper.selectOne(
                new LambdaQueryWrapper<SopExecution>()
                    .eq(SopExecution::getExecutorId, userId)
                    .eq(SopExecution::getSopId, sopId)
                    .in(SopExecution::getStatus, "pending", "in_progress")
            );
            if (existing != null) return existing;
        }

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
        e.setOrgId(sop.getOrgId()); // Copy OrgId from SOP
        e.setSopId(sopId);
        e.setSopVersion(sop.getVersion());
        e.setExecutorId(userId);
        e.setStatus("pending");
        e.setCurrentStep(0);
        e.setStartedAt(LocalDateTime.now());
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        executionMapper.insert(e);

        notificationService.createAndDispatch(userId, "execution_started",
                "SOP 开始执行", "您已开始执行 SOP《" + sop.getTitle() + "》，记得完成所有步骤哦！", "sop", sop.getId());

        return e;
    }

    @Override
    @Transactional
    public boolean completeStep(Long userId, Long executionId, int stepIndex, String notes,
                                 Map<String, Object> checkData, String guidance) {
        SopExecution exec = getExecution(executionId);
        if (!exec.getExecutorId().equals(userId)) throw new RuntimeException("无权操作");
        if (!"in_progress".equals(exec.getStatus())) throw new RuntimeException("执行状态不允许操作");

        Sop sop = sopMapper.selectById(exec.getSopId());
        List<?> steps = parseJson(sop.getContent(), List.class);
        if (steps == null) steps = Collections.emptyList();
        if (stepIndex < 1 || stepIndex > steps.size()) throw new RuntimeException("步骤序号无效: " + stepIndex);

        ExecutionStepRecord existing = stepRecordMapper.selectOne(
            new LambdaQueryWrapper<ExecutionStepRecord>()
                .eq(ExecutionStepRecord::getExecutionId, executionId)
                .eq(ExecutionStepRecord::getStepIndex, stepIndex)
        );
        if (existing != null) return stepIndex >= steps.size();

        ExecutionStepRecord record = new ExecutionStepRecord();
        record.setExecutionId(executionId);
        record.setStepIndex(stepIndex);
        Map<?, ?> step = (Map<?, ?>) steps.get(stepIndex - 1);
        record.setStepTitle((String) step.get("title"));
        record.setCompletedAt(LocalDateTime.now());
        record.setNotes(notes);
        record.setGuidance(guidance);
        if (checkData != null) {
            try { record.setCheckData(objectMapper.writeValueAsString(checkData)); } catch (Exception ex) { record.setCheckData("{}"); }
        }
        stepRecordMapper.insert(record);

        boolean completed = stepIndex >= steps.size();
        if (completed) {
            exec.setStatus("completed");
            exec.setCompletedAt(LocalDateTime.now());
            exec.setCurrentStep(stepIndex);
            notificationService.createAndDispatch(userId, "execution_completed",
                    "SOP 执行完成", "您执行的 SOP《" + sop.getTitle() + "》已全部完成，干得漂亮！", "sop", sop.getId());
            
            // Pass OrgId to gamification
            gamificationService.onExecutionCompleted(userId, exec.getOrgId(), exec.getSopId());
        } else {
            exec.setCurrentStep(stepIndex + 1);
            notificationService.createAndDispatch(userId, "step_completed",
                    "步骤完成", "SOP《" + sop.getTitle() + "》第 " + stepIndex + " 步已完成，继续加油！", "sop", sop.getId());
        }
        exec.setUpdatedAt(LocalDateTime.now());
        executionMapper.updateById(exec);
        return completed;
    }

    @Override
    @Transactional
    public void activateExecution(Long userId, Long executionId) {
        SopExecution exec = getExecution(executionId);
        if (!exec.getExecutorId().equals(userId)) throw new RuntimeException("无权操作");
        if (!"pending".equals(exec.getStatus())) return;
        exec.setStatus("in_progress");
        exec.setCurrentStep(1);
        exec.setStartedAt(LocalDateTime.now());
        exec.setUpdatedAt(LocalDateTime.now());
        executionMapper.updateById(exec);
    }

    @Override
    @Transactional
    public void finishExecution(Long userId, Long executionId) {
        SopExecution exec = getExecution(executionId);
        if (!exec.getExecutorId().equals(userId)) throw new RuntimeException("无权操作");

        exec.setStatus("completed");
        exec.setCompletedAt(LocalDateTime.now());
        exec.setUpdatedAt(LocalDateTime.now());
        executionMapper.updateById(exec);
        Sop sop = sopMapper.selectById(exec.getSopId());
        notificationService.createAndDispatch(userId, "execution_completed",
                "SOP 执行完成", "SOP《" + (sop != null ? sop.getTitle() : "") + "》已标记完成！", "sop", exec.getSopId());

        gamificationService.onExecutionCompleted(userId, exec.getOrgId(), exec.getSopId());
    }

    @Override
    @Transactional
    public int undoLastStep(Long userId, Long executionId) {
        SopExecution exec = getExecution(executionId);
        if (!exec.getExecutorId().equals(userId)) throw new RuntimeException("无权操作");
        if (!"in_progress".equals(exec.getStatus())) throw new RuntimeException("当前状态不允许撤销");
        if (exec.getCurrentStep() == null || exec.getCurrentStep() <= 0) return 0;

        ExecutionStepRecord lastRecord = stepRecordMapper.selectOne(
            new LambdaQueryWrapper<ExecutionStepRecord>()
                .eq(ExecutionStepRecord::getExecutionId, executionId)
                .orderByDesc(ExecutionStepRecord::getStepIndex)
                .last("LIMIT 1")
        );
        if (lastRecord == null) return 0;

        int undoneStepIndex = lastRecord.getStepIndex();
        stepRecordMapper.deleteById(lastRecord.getId());
        exec.setCurrentStep(Math.max(1, undoneStepIndex - 1));
        exec.setUpdatedAt(LocalDateTime.now());
        executionMapper.updateById(exec);
        return undoneStepIndex;
    }

    @Override
    public Page<SopExecution> getMyExecutions(Long userId, String status, int page, int pageSize) {
        LambdaQueryWrapper<SopExecution> q = new LambdaQueryWrapper<SopExecution>()
                .eq(SopExecution::getExecutorId, userId)
                .orderByDesc(SopExecution::getCreatedAt);
        if (status != null && !status.isEmpty()) q.eq(SopExecution::getStatus, status);
        Page<SopExecution> p = new Page<>(page, pageSize);
        return executionMapper.selectPage(p, q);
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
        try { return objectMapper.readValue(json, clazz); } catch (Exception e) { return null; }
    }
}
