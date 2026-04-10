package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.*;
import com.biaofan.mapper.*;
import com.biaofan.service.GamificationService;
import com.biaofan.service.SopExecutionService;
import com.biaofan.service.NotificationDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SOP执行服务实现类
 * 管理SOP的执行生命周期：开始、执行步骤、完成
 * 执行过程中发送站内通知，完成后触发积分化和成就检查
 *
 * @author biaofan
 */

/**
 * SOP 执行单服务实现
 * - 执行单 CRUD
 * - 激活执行单（pending → in_progress）
 * - 步骤打卡（校验检查项、更新步骤状态）
 * - 完成执行单（所有步骤完成后）
 * - 步骤完成时自动触发积分奖励
 */
@Service
public class SopExecutionServiceImpl implements SopExecutionService {

    private final SopExecutionMapper executionMapper;
    private final SopMapper sopMapper;
    private final ExecutionStepRecordMapper stepRecordMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationDispatcher notificationDispatcher;
    private final GamificationService gamificationService;
    private final ObjectMapper objectMapper;

    public SopExecutionServiceImpl(
            SopExecutionMapper executionMapper,
            SopMapper sopMapper,
            ExecutionStepRecordMapper stepRecordMapper,
            NotificationMapper notificationMapper,
            NotificationDispatcher notificationDispatcher,
            GamificationService gamificationService) {
        this.executionMapper = executionMapper;
        this.sopMapper = sopMapper;
        this.stepRecordMapper = stepRecordMapper;
        this.notificationMapper = notificationMapper;
        this.notificationDispatcher = notificationDispatcher;
        this.gamificationService = gamificationService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 开始执行SOP
     * 校验SOP已发布，如已有进行中的执行则返回该执行
     * @param userId 执行人ID
     * @param sopId SOP ID
     * @return 创建的执行记录
     */
    @Override
    @Transactional
    public SopExecution startExecution(Long userId, Long sopId) {
        Sop sop = sopMapper.selectById(sopId);
        if (sop == null) {
            throw new RuntimeException("SOP不存在");
        }
        if (!"published".equals(sop.getStatus())) {
            throw new RuntimeException("SOP未发布");
        }

        SopExecution ongoing = executionMapper.selectOne(
            new LambdaQueryWrapper<SopExecution>()
                .eq(SopExecution::getExecutorId, userId)
                .eq(SopExecution::getSopId, sopId)
                .in(SopExecution::getStatus, "pending", "in_progress")
        );
        if (ongoing != null) {
            return ongoing;
        }

        SopExecution e = new SopExecution();
        e.setSopId(sopId);
        e.setSopVersion(sop.getVersion());
        e.setExecutorId(userId);
        e.setStatus("pending");
        e.setCurrentStep(0);
        e.setStartedAt(LocalDateTime.now());
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        executionMapper.insert(e);

        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setType("execution_started");
        notif.setTitle("SOP 开始执行");
        notif.setContent("您已开始执行 SOP《" + sop.getTitle() + "》，记得完成所有步骤哦！");
        notif.setSourceType("sop");
        notif.setSourceId(sop.getId());
        notif.setIsRead(0);
        notif.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notif);
        notificationDispatcher.dispatch(userId, notif.getTitle(), notif.getContent());

        return e;
    }

    /**
     * 完成执行中的某一步骤
     * 记录步骤完成情况，更新当前步骤号，判断是否全部完成
     * @param userId 执行人ID
     * @param executionId 执行记录ID
     * @param stepIndex 步骤序号（从1开始）
     * @param notes 步骤备注
     * @param checkData 校验数据（JSON）
     * @return 是否全部完成
     */
    @Override
    @Transactional
    public boolean completeStep(Long userId, Long executionId, int stepIndex, String notes,
                                 Map<String, Object> checkData) {
        SopExecution exec = getExecution(executionId);
        if (!exec.getExecutorId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"in_progress".equals(exec.getStatus())) {
            throw new RuntimeException("执行状态不允许操作");
        }

        Sop sop = sopMapper.selectById(exec.getSopId());
        List<?> steps = parseJson(sop.getContent(), List.class);
        if (steps == null) {
            steps = Collections.emptyList();
        }

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
            } catch (Exception ex) {
                record.setCheckData("{}");
            }
        }
        stepRecordMapper.insert(record);

        boolean completed = stepIndex >= steps.size();
        if (completed) {
            exec.setStatus("completed");
            exec.setCompletedAt(LocalDateTime.now());
            exec.setCurrentStep(stepIndex);
            Notification notif = new Notification();
            notif.setUserId(userId);
            notif.setType("execution_completed");
            notif.setTitle("SOP 执行完成");
            notif.setContent("您执行的 SOP《" + sop.getTitle() + "》已全部完成，干得漂亮！");
            notif.setSourceType("sop");
            notif.setSourceId(sop.getId());
            notif.setIsRead(0);
            notif.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(notif);
            notificationDispatcher.dispatch(userId, notif.getTitle(), notif.getContent());
        } else {
            exec.setCurrentStep(stepIndex + 1);
            Notification notif = new Notification();
            notif.setUserId(userId);
            notif.setType("step_completed");
            notif.setTitle("步骤完成");
            notif.setContent("SOP《" + sop.getTitle() + "》第 " + stepIndex + " 步已完成，继续加油！");
            notif.setSourceType("sop");
            notif.setSourceId(sop.getId());
            notif.setIsRead(0);
            notif.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(notif);
            notificationDispatcher.dispatch(userId, notif.getTitle(), notif.getContent());
        }
        exec.setUpdatedAt(LocalDateTime.now());
        executionMapper.updateById(exec);
        return completed;
    }

    /**
     * 激活执行（从待执行变为进行中）
     * @param userId 执行人ID
     * @param executionId 执行记录ID
     */
    @Override
    @Transactional
    public void activateExecution(Long userId, Long executionId) {
        SopExecution exec = getExecution(executionId);
        if (!exec.getExecutorId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"pending".equals(exec.getStatus())) {
            return;
        }
        exec.setStatus("in_progress");
        exec.setCurrentStep(1);
        exec.setStartedAt(LocalDateTime.now());
        exec.setUpdatedAt(LocalDateTime.now());
        executionMapper.updateById(exec);
    }

    /**
     * 手动标记执行完成
     * 完成后触发积分化（增加经验和积分）
     * @param userId 执行人ID
     * @param executionId 执行记录ID
     */
    @Override
    @Transactional
    public void finishExecution(Long userId, Long executionId) {
        SopExecution exec = getExecution(executionId);
        if (!exec.getExecutorId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        exec.setStatus("completed");
        exec.setCompletedAt(LocalDateTime.now());
        exec.setUpdatedAt(LocalDateTime.now());
        executionMapper.updateById(exec);

        Sop sop = sopMapper.selectById(exec.getSopId());
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setType("execution_completed");
        notif.setTitle("SOP 执行完成");
        notif.setContent("SOP《" + sop.getTitle() + "》已标记完成！");
        notif.setSourceType("sop");
        notif.setSourceId(sop.getId());
        notif.setIsRead(0);
        notif.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notif);
        notificationDispatcher.dispatch(userId, notif.getTitle(), notif.getContent());

        // Update gamification: points, exp, badges, streak
        gamificationService.onExecutionCompleted(userId, exec.getSopId());
    }

    /**
     * 查询当前用户的执行记录列表
     * @param userId 用户ID
     * @param status 状态过滤（可选，如pending/in_progress/completed）
     * @return 执行记录列表
     */
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

    /**
     * 获取执行记录详情
     * @param executionId 执行记录ID
     * @return 执行记录实体
     */
    @Override
    public SopExecution getExecution(Long executionId) {
        SopExecution e = executionMapper.selectById(executionId);
        if (e == null) {
            throw new RuntimeException("执行记录不存在");
        }
        return e;
    }

    /**
     * 获取SOP及其包含的步骤
     * @param sopId SOP ID
     * @return SOP实体
     */
    @Override
    public Sop getSopWithSteps(Long sopId) {
        return sopMapper.selectById(sopId);
    }

    /**
     * 获取执行记录的步骤完成历史
     * @param executionId 执行记录ID
     * @return 步骤记录列表，按步骤序号升序
     */
    @Override
    public List<ExecutionStepRecord> getStepRecords(Long executionId) {
        return stepRecordMapper.selectList(
            new LambdaQueryWrapper<ExecutionStepRecord>()
                .eq(ExecutionStepRecord::getExecutionId, executionId)
                .orderByAsc(ExecutionStepRecord::getStepIndex)
        );
    }

    /**
     * 获取指定执行的总步骤数
     * @param executionId 执行记录ID
     * @return 步骤总数
     */
    @Override
    public int getStepCount(Long executionId) {
        SopExecution e = getExecution(executionId);
        Sop sop = sopMapper.selectById(e.getSopId());
        List<?> steps = parseJson(sop.getContent(), List.class);
        if (steps != null) {
            return steps.size();
        }
        return 0;
    }

    /**
     * 解析JSON字符串
     * @param json JSON字符串
     * @param clazz 目标类型
     * @return 解析后的对象，解析失败返回null
     */
    private <T> T parseJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }
}
