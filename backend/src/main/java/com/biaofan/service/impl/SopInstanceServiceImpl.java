package com.biaofan.service.impl;


/**
 * 周期实例服务实现
 * - SopInstance 的创建、激活、完成
 * - 步骤打卡（针对周期实例）
 * - 定时任务 ScheduleTaskJob 调用本服务创建实例
 */
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.biaofan.entity.*;
import com.biaofan.mapper.*;
import com.biaofan.service.GamificationService;
import com.biaofan.service.NotificationDispatcher;
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
 * 管理周期性SOP（每日/每周/每月/每年）的自动实例生成
 * 支持实例激活、步骤完成、逾期标记等操作，完成后触发积分化
 *
 * @author biaofan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SopInstanceServiceImpl implements SopInstanceService {

    private final SopInstanceMapper instanceMapper;
    private final SopMapper sopMapper;
    private final SopExecutionMapper executionMapper;
    private final ExecutionStepRecordMapper stepRecordMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationDispatcher notificationDispatcher;
    private final GamificationService gamificationService;
    private final ObjectMapper objectMapper;

    /**
     * 查询当前用户的周期性实例列表
     * @param userId 用户ID
     * @param status 状态过滤（可选）
     * @return 实例列表
     */
    @Override
    public List<SopInstance> getMyInstances(Long userId, String status) {
        LambdaQueryWrapper<SopInstance> q = new LambdaQueryWrapper<SopInstance>()
                .eq(SopInstance::getExecutorId, userId)
                .orderByDesc(SopInstance::getCreatedAt);
        if (status != null && !status.isEmpty()) {
            q.eq(SopInstance::getStatus, status);
        }
        return instanceMapper.selectList(q);
    }

    /**
     * 获取实例详情
     * @param instanceId 实例ID
     * @return 实例实体
     */
    @Override
    public SopInstance getInstance(Long instanceId) {
        SopInstance inst = instanceMapper.selectById(instanceId);
        if (inst == null) {
            throw new RuntimeException("实例不存在");
        }
        return inst;
    }

    /**
     * 激活实例并创建执行记录
     * @param userId 执行人ID
     * @param instanceId 实例ID
     */
    @Override
    @Transactional
    public void activateInstance(Long userId, Long instanceId) {
        SopInstance inst = getInstance(instanceId);
        if (!inst.getExecutorId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"pending".equals(inst.getStatus())) {
            return;
        }

        inst.setStatus("in_progress");
        inst.setCurrentStep(1);
        inst.setStartedAt(LocalDateTime.now());
        inst.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(inst);

        SopExecution exec = new SopExecution();
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
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setType("execution_started");
        notif.setTitle("SOP 开始执行");
        notif.setContent("您已开始执行 SOP《" + (sop != null ? sop.getTitle() : "") + "》");
        notif.setSourceType("sop");
        notif.setSourceId(inst.getSopId());
        notif.setIsRead(0);
        notif.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notif);
        notificationDispatcher.dispatch(userId, notif.getTitle(), notif.getContent());
    }

    /**
     * 完成实例中的步骤
     * @param userId 执行人ID
     * @param instanceId 实例ID
     * @param stepIndex 步骤序号（从1开始）
     * @param notes 步骤备注
     * @param checkData 校验数据
     * @return 是否全部完成
     */
    @Override
    @Transactional
    public boolean completeStep(Long userId, Long instanceId, int stepIndex, String notes,
                                 Map<String, Object> checkData) {
        SopInstance inst = getInstance(instanceId);
        if (!inst.getExecutorId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"in_progress".equals(inst.getStatus())) {
            throw new RuntimeException("实例状态不允许操作");
        }

        Sop sop = sopMapper.selectById(inst.getSopId());
        List<?> steps = parseJson(sop.getContent(), List.class);
        if (steps == null) {
            steps = Collections.emptyList();
        }

        SopExecution latestExec = getLatestExecution(userId, inst.getSopId());
        if (latestExec != null) {
            ExecutionStepRecord record = new ExecutionStepRecord();
            record.setExecutionId(latestExec.getId());
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
                } catch (Exception e) {
                    record.setCheckData("{}");
                }
            }
            stepRecordMapper.insert(record);
        }

        boolean completed = stepIndex >= steps.size();
        if (completed) {
            inst.setStatus("completed");
            inst.setCompletedAt(LocalDateTime.now());
            inst.setCurrentStep(stepIndex);
            if (latestExec != null) {
                latestExec.setStatus("completed");
                latestExec.setCompletedAt(LocalDateTime.now());
                latestExec.setCurrentStep(stepIndex);
                latestExec.setUpdatedAt(LocalDateTime.now());
                executionMapper.updateById(latestExec);
            }
            Notification notif = new Notification();
            notif.setUserId(userId);
            notif.setType("execution_completed");
            notif.setTitle("SOP 执行完成");
            notif.setContent("您执行的 SOP《" + sop.getTitle() + "》已全部完成！");
            notif.setSourceType("sop");
            notif.setSourceId(sop.getId());
            notif.setIsRead(0);
            notif.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(notif);
            notificationDispatcher.dispatch(userId, notif.getTitle(), notif.getContent());
        } else {
            inst.setCurrentStep(stepIndex + 1);
            if (latestExec != null) {
                latestExec.setCurrentStep(stepIndex + 1);
                latestExec.setUpdatedAt(LocalDateTime.now());
                executionMapper.updateById(latestExec);
            }
        }
        inst.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(inst);
        return completed;
    }

    /**
     * 手动标记实例完成
     * @param userId 执行人ID
     * @param instanceId 实例ID
     */
    @Override
    @Transactional
    public void finishInstance(Long userId, Long instanceId) {
        SopInstance inst = getInstance(instanceId);
        if (!inst.getExecutorId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        inst.setStatus("completed");
        inst.setCompletedAt(LocalDateTime.now());
        inst.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(inst);

        Sop sop = sopMapper.selectById(inst.getSopId());
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setType("execution_completed");
        notif.setTitle("SOP 执行完成");
        notif.setContent("SOP《" + (sop != null ? sop.getTitle() : "") + "》已标记完成！");
        notif.setSourceType("sop");
        notif.setSourceId(inst.getSopId());
        notif.setIsRead(0);
        notif.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notif);
        notificationDispatcher.dispatch(userId, notif.getTitle(), notif.getContent());

        // Update gamification: points, exp, badges, streak
        gamificationService.onExecutionCompleted(userId, inst.getSopId());
    }

    /**
     * 生成周期性实例（每日/每周/每月/每年）
     * 根据SOP分类生成对应周期的实例，已存在则跳过
     */
    @Override
    @Transactional
    public void generatePeriodicInstances() {
        LocalDate today = LocalDate.now();
        List<Sop> publishedSops = sopMapper.selectList(
                new LambdaQueryWrapper<Sop>()
                        .eq(Sop::getStatus, "published")
                        .in(Sop::getCategory, "daily", "weekly", "monthly", "yearly"));

        for (Sop sop : publishedSops) {
            try {
                generateForSop(sop, today);
            } catch (Exception e) {
                log.error("生成实例失败 sopId={} error={}", sop.getId(), e.getMessage());
            }
        }
    }

    /**
     * 为单个SOP生成实例
     * @param sop SOP实体
     * @param date 参考日期
     */
    private void generateForSop(Sop sop, LocalDate date) {
        String category = sop.getCategory();
        LocalDateTime periodStart;
        LocalDateTime periodEnd;

        switch (category) {
            case "daily":
                periodStart = date.atStartOfDay();
                periodEnd = date.atTime(23, 59, 59);
                break;
            case "weekly":
                periodStart = date.with(DayOfWeek.MONDAY).atStartOfDay();
                periodEnd = date.with(DayOfWeek.SUNDAY).atTime(23, 59, 59);
                break;
            case "monthly":
                periodStart = date.withDayOfMonth(1).atStartOfDay();
                periodEnd = date.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
                break;
            case "yearly":
                periodStart = date.withDayOfYear(1).atStartOfDay();
                periodEnd = date.with(TemporalAdjusters.lastDayOfYear()).atTime(23, 59, 59);
                break;
            default:
                return;
        }

        Long userId = sop.getUserId();
        Long existing = instanceMapper.selectCount(
                new LambdaQueryWrapper<SopInstance>()
                        .eq(SopInstance::getSopId, sop.getId())
                        .eq(SopInstance::getExecutorId, userId)
                        .eq(SopInstance::getPeriodStart, periodStart)
                        .eq(SopInstance::getPeriodEnd, periodEnd));
        if (existing > 0) {
            return;
        }

        SopInstance inst = new SopInstance();
        inst.setSopId(sop.getId());
        inst.setSopVersion(sop.getVersion() != null ? sop.getVersion() : 1);
        inst.setExecutorId(userId);
        inst.setPeriodStart(periodStart);
        inst.setPeriodEnd(periodEnd);
        inst.setStatus("pending");
        inst.setCurrentStep(0);
        inst.setCreatedAt(LocalDateTime.now());
        inst.setUpdatedAt(LocalDateTime.now());
        instanceMapper.insert(inst);

        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setType("schedule_triggered");
        notif.setTitle("SOP 待执行提醒");
        notif.setContent("您的 " + categoryLabel(category) + " SOP《" + sop.getTitle() + "》已生成，请在 " + periodEnd.toLocalDate() + " 前完成。");
        notif.setSourceType("sop");
        notif.setSourceId(sop.getId());
        notif.setIsRead(0);
        notif.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notif);
        notificationDispatcher.dispatch(userId, notif.getTitle(), notif.getContent());

        log.info("生成实例: sopId={} category={} period={}~{}", sop.getId(), category, periodStart, periodEnd);
    }

    /**
     * 标记逾期实例
     * 将periodEnd已过但状态仍为pending/in_progress的实例标记为overdue
     */
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
            Notification notif = new Notification();
            notif.setUserId(inst.getExecutorId());
            notif.setType("execution_overdue");
            notif.setTitle("SOP 执行逾期");
            notif.setContent("您的 SOP《" + (sop != null ? sop.getTitle() : "") + "》已超过执行期限，请尽快处理！");
            notif.setSourceType("sop");
            notif.setSourceId(inst.getSopId());
            notif.setIsRead(0);
            notif.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(notif);
            notificationDispatcher.dispatch(inst.getExecutorId(), notif.getTitle(), notif.getContent());
        }

        if (!overdue.isEmpty()) {
            log.info("标记 {} 条逾期实例", overdue.size());
        }
    }

    /**
     * 获取用户对指定SOP的最新执行记录
     */
    private SopExecution getLatestExecution(Long userId, Long sopId) {
        return executionMapper.selectOne(
                new LambdaQueryWrapper<SopExecution>()
                        .eq(SopExecution::getExecutorId, userId)
                        .eq(SopExecution::getSopId, sopId)
                        .orderByDesc(SopExecution::getCreatedAt)
                        .last("LIMIT 1"));
    }

    /**
     * 获取周期分类的中文标签
     */
    private String categoryLabel(String category) {
        return switch (category) {
            case "daily" -> "日";
            case "weekly" -> "周";
            case "monthly" -> "月";
            case "yearly" -> "年";
            default -> "";
        };
    }

    /**
     * 解析JSON字符串
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
