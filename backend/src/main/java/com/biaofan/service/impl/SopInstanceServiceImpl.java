package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.*;
import com.biaofan.mapper.*;
import com.biaofan.service.GamificationService;
import com.biaofan.service.NotificationService;
import com.biaofan.service.SopInstanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * SOP实例服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SopInstanceServiceImpl implements SopInstanceService {

    private final SopInstanceMapper instanceMapper;
    private final SopMapper sopMapper;
    private final SopExecutionMapper executionMapper;
    private final ExecutionStepRecordMapper stepRecordMapper;
    private final NotificationService notificationService;
    private final GamificationService gamificationService;
    private final ObjectMapper objectMapper;

    @Override
    public Page<SopInstance> getMyInstances(Long userId, String status, int page, int pageSize) {
        LambdaQueryWrapper<SopInstance> q = new LambdaQueryWrapper<SopInstance>()
                .eq(SopInstance::getExecutorId, userId)
                .orderByDesc(SopInstance::getCreatedAt);
        if (status != null && !status.isEmpty()) {
            q.eq(SopInstance::getStatus, status);
        }
        Page<SopInstance> p = new Page<>(page, pageSize);
        return instanceMapper.selectPage(p, q);
    }

    @Override
    @Transactional(readOnly = true)
    public SopInstance getInstance(Long instanceId) {
        SopInstance inst = instanceMapper.selectById(instanceId);
        if (inst == null) throw new RuntimeException("实例不存在");
        return inst;
    }

    @Override
    public Sop getSopByInstanceId(Long instanceId) {
        SopInstance inst = getInstance(instanceId);
        return sopMapper.selectById(inst.getSopId());
    }

    @Override
    @Transactional
    public void activateInstance(Long userId, Long instanceId) {
        SopInstance inst = getInstance(instanceId);
        if (!inst.getExecutorId().equals(userId)) throw new RuntimeException("无权操作");
        if (!"pending".equals(inst.getStatus())) return;

        inst.setStatus("in_progress");
        inst.setCurrentStep(1);
        inst.setStartedAt(LocalDateTime.now());
        inst.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(inst);

        SopExecution exec = new SopExecution();
        exec.setOrgId(inst.getOrgId()); // Set OrgId
        exec.setSopId(inst.getSopId());
        exec.setSopVersion(inst.getSopVersion());
        exec.setExecutorId(userId);
        exec.setStatus("in_progress");
        exec.setCurrentStep(1);
        exec.setStartedAt(LocalDateTime.now());
        exec.setCreatedAt(LocalDateTime.now());
        exec.setUpdatedAt(LocalDateTime.now());
        executionMapper.insert(exec);

        Sop sop = sopMapper.selectById(inst.getSopId());
        notificationService.createAndDispatch(
                userId,
                "execution_started",
                "SOP 开始执行",
                "您已开始执行 SOP《" + (sop != null ? sop.getTitle() : "") + "》",
                "sop",
                inst.getSopId());
    }

    @Override
    @Transactional
    public boolean completeStep(Long userId, Long instanceId, int stepIndex, String notes,
                                 Map<String, Object> checkData, String guidance) {
        SopInstance inst = getInstance(instanceId);
        if (!inst.getExecutorId().equals(userId)) throw new RuntimeException("无权操作");
        if (!"in_progress".equals(inst.getStatus())) throw new RuntimeException("实例状态不允许操作");

        Sop sop = sopMapper.selectById(inst.getSopId());
        if (sop == null) throw new RuntimeException("SOP已删除");
        List<?> steps = parseJson(sop.getContent(), List.class);
        if (stepIndex < 1 || stepIndex > steps.size()) throw new RuntimeException("步骤序号无效: " + stepIndex);

        SopExecution latestExec = getLatestExecution(userId, inst.getSopId());
        if (latestExec == null) throw new RuntimeException("无执行记录");
        
        ExecutionStepRecord existing = stepRecordMapper.selectOne(
            new LambdaQueryWrapper<ExecutionStepRecord>()
                .eq(ExecutionStepRecord::getExecutionId, latestExec.getId())
                .eq(ExecutionStepRecord::getStepIndex, stepIndex)
        );
        if (existing != null) return stepIndex >= steps.size();

        ExecutionStepRecord record = new ExecutionStepRecord();
        record.setExecutionId(latestExec.getId());
        record.setStepIndex(stepIndex);
        Object stepObj = steps.get(stepIndex - 1);
        if (stepObj instanceof Map) {
            record.setStepTitle((String) ((Map<?, ?>) stepObj).get("title"));
        }
        record.setCompletedAt(LocalDateTime.now());
        record.setNotes(notes);
        record.setGuidance(guidance);
        if (checkData != null) {
            try { record.setCheckData(objectMapper.writeValueAsString(checkData)); } catch (Exception e) { record.setCheckData("{}"); }
        }
        stepRecordMapper.insert(record);

        boolean completed = stepIndex >= steps.size();
        if (completed) {
            inst.setStatus("completed");
            inst.setCompletedAt(LocalDateTime.now());
            inst.setCurrentStep(stepIndex);
            latestExec.setStatus("completed");
            latestExec.setCompletedAt(LocalDateTime.now());
            latestExec.setCurrentStep(stepIndex);
            latestExec.setUpdatedAt(LocalDateTime.now());
            executionMapper.updateById(latestExec);
            
            notificationService.createAndDispatch(userId, "execution_completed", "SOP 执行完成",
                    "您执行的 SOP《" + (sop != null ? sop.getTitle() : "已删除SOP") + "》已全部完成！", "sop", sop.getId());
            
            // Trigger gamification on last step
            gamificationService.onExecutionCompleted(userId, inst.getOrgId(), inst.getSopId());
        } else {
            inst.setCurrentStep(stepIndex + 1);
            latestExec.setCurrentStep(stepIndex + 1);
            latestExec.setUpdatedAt(LocalDateTime.now());
            executionMapper.updateById(latestExec);
        }
        inst.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(inst);
        return completed;
    }

    @Override
    @Transactional
    public void finishInstance(Long userId, Long instanceId) {
        SopInstance inst = getInstance(instanceId);
        if (!inst.getExecutorId().equals(userId)) throw new RuntimeException("无权操作");
        if ("completed".equals(inst.getStatus())) return;
        
        inst.setStatus("completed");
        inst.setCompletedAt(LocalDateTime.now());
        inst.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(inst);

        Sop sop = sopMapper.selectById(inst.getSopId());
        notificationService.createAndDispatch(userId, "execution_completed", "SOP 执行完成",
                "SOP《" + (sop != null ? sop.getTitle() : "") + "》已标记完成！", "sop", inst.getSopId());

        gamificationService.onExecutionCompleted(userId, inst.getOrgId(), inst.getSopId());
    }

    @Override
    @Transactional
    public int undoLastStep(Long userId, Long instanceId) {
        SopInstance inst = getInstance(instanceId);
        if (!inst.getExecutorId().equals(userId)) throw new RuntimeException("无权操作");
        if (!"in_progress".equals(inst.getStatus())) throw new RuntimeException("当前状态不允许撤销");

        SopExecution latestExec = getLatestExecution(userId, inst.getSopId());
        if (latestExec == null) return 0;

        ExecutionStepRecord lastRecord = stepRecordMapper.selectOne(
            new LambdaQueryWrapper<ExecutionStepRecord>()
                .eq(ExecutionStepRecord::getExecutionId, latestExec.getId())
                .orderByDesc(ExecutionStepRecord::getStepIndex)
                .last("LIMIT 1")
        );
        if (lastRecord == null) return 0;

        int undoneStepIndex = lastRecord.getStepIndex();
        stepRecordMapper.deleteById(lastRecord.getId());
        int newStep = Math.max(1, undoneStepIndex - 1);
        inst.setCurrentStep(newStep);
        inst.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(inst);

        latestExec.setCurrentStep(newStep);
        latestExec.setUpdatedAt(LocalDateTime.now());
        executionMapper.updateById(latestExec);

        return undoneStepIndex;
    }

    @Override
    @Transactional
    public void generatePeriodicInstances() {
        LocalDate today = LocalDate.now();
        List<Sop> publishedSops = sopMapper.selectList(
                new LambdaQueryWrapper<Sop>()
                        .eq(Sop::getStatus, "published")
                        .in(Sop::getCategory, "daily", "weekly", "monthly", "yearly"));

        for (Sop sop : publishedSops) {
            generateForSop(sop, today);
        }
    }

    private void generateForSop(Sop sop, LocalDate date) {
        String category = sop.getCategory();
        LocalDateTime periodStart;
        LocalDateTime periodEnd;

        switch (category) {
            case "daily": periodStart = date.atStartOfDay(); periodEnd = date.atTime(23, 59, 59); break;
            case "weekly": periodStart = date.with(DayOfWeek.MONDAY).atStartOfDay(); periodEnd = date.with(DayOfWeek.SUNDAY).atTime(23, 59, 59); break;
            case "monthly": periodStart = date.withDayOfMonth(1).atStartOfDay(); periodEnd = date.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59); break;
            case "yearly": periodStart = date.withDayOfYear(1).atStartOfDay(); periodEnd = date.with(TemporalAdjusters.lastDayOfYear()).atTime(23, 59, 59); break;
            default: return;
        }

        SopInstance inst = new SopInstance();
        inst.setOrgId(sop.getOrgId()); // Copy OrgId from SOP
        inst.setSopId(sop.getId());
        inst.setSopVersion(sop.getVersion() != null ? sop.getVersion() : 1);
        inst.setExecutorId(sop.getUserId());
        inst.setPeriodStart(periodStart);
        inst.setPeriodEnd(periodEnd);
        inst.setStatus("pending");
        inst.setCurrentStep(0);
        inst.setCreatedAt(LocalDateTime.now());
        inst.setUpdatedAt(LocalDateTime.now());
        
        if (instanceMapper.insertIgnore(inst) == 0) return;

        notificationService.createAndDispatch(sop.getUserId(), "schedule_triggered", "SOP 待执行提醒",
                "您的 " + categoryLabel(category) + " SOP《" + sop.getTitle() + "》已生成，请在 " + periodEnd.toLocalDate() + " 前完成。",
                "sop", sop.getId());
    }

    @Override
    @Transactional
    public void markOverdueInstances() {
        List<SopInstance> overdue = instanceMapper.selectList(
                new LambdaQueryWrapper<SopInstance>()
                        .in(SopInstance::getStatus, "pending", "in_progress")
                        .lt(SopInstance::getPeriodEnd, LocalDateTime.now()));

        for (SopInstance inst : overdue) {
            inst.setStatus("overdue");
            inst.setUpdatedAt(LocalDateTime.now());
            instanceMapper.updateById(inst);

            Sop sop = sopMapper.selectById(inst.getSopId());
            notificationService.createAndDispatch(inst.getExecutorId(), "execution_overdue", "SOP 执行逾期",
                    "SOP《" + (sop != null ? sop.getTitle() : "（已删除）") + "》已超过执行期限", "sop", inst.getSopId());
        }
    }

    private SopExecution getLatestExecution(Long userId, Long sopId) {
        return executionMapper.selectOne(
                new LambdaQueryWrapper<SopExecution>()
                        .eq(SopExecution::getExecutorId, userId)
                        .eq(SopExecution::getSopId, sopId)
                        .orderByDesc(SopExecution::getCreatedAt)
                        .last("LIMIT 1"));
    }

    private String categoryLabel(String category) {
        return switch (category) { case "daily" -> "日"; case "weekly" -> "周"; case "monthly" -> "月"; case "yearly" -> "年"; default -> ""; };
    }

    private <T> T parseJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return null;
        try { return objectMapper.readValue(json, clazz); } catch (Exception e) { return null; }
    }
}
