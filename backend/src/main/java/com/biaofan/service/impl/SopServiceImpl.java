package com.biaofan.service.impl;


/**
 * SOP 主体服务实现
 * - SOP 的 CRUD、软删除
 * - 版本管理（创建新版本、版本列表）
 * - 发布 SOP（状态变更）
 * - 分页搜索
 */
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.*;
import com.biaofan.dto.SopRequest;
import com.biaofan.mapper.*;
import com.biaofan.service.SopService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SOP（标准操作程序）服务实现类
 * 提供SOP的创建、更新、删除、发布及版本快照管理
 * 支持草稿状态管理，发布时自动创建版本快照
 *
 * @author biaofan
 */
@Service
@RequiredArgsConstructor
public class SopServiceImpl implements SopService {

    private final SopMapper sopMapper;
    private final SopVersionMapper versionMapper;
    private final SopExecutionMapper executionMapper;
    private final ExecutionStepRecordMapper stepRecordMapper;
    private final ExecutionStatMapper executionStatMapper;
    private final ScheduleTaskMapper scheduleTaskMapper;
    private final SopDraftMapper draftMapper;
    private final SopExceptionMapper exceptionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 分页查询当前用户的SOP列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return SOP分页结果，按更新时间倒序
     */
    @Override
    public IPage<Sop> getMySops(Long userId, int page, int size) {
        Page<Sop> p = new Page<>(page, size);
        LambdaQueryWrapper<Sop> q = new LambdaQueryWrapper<Sop>()
                .eq(Sop::getUserId, userId)
                .orderByDesc(Sop::getUpdatedAt);
        return sopMapper.selectPage(p, q);
    }

    /**
     * 根据ID获取SOP详情
     * @param id SOP ID
     * @param userId 用户ID，用于权限校验
     * @return SOP实体
     */
    @Override
    public Sop getById(Long id, Long userId) {
        Sop sop = sopMapper.selectById(id);
        if (sop == null) throw new RuntimeException("SOP不存在");
        if (!sop.getUserId().equals(userId)) throw new RuntimeException("无权访问");
        return sop;
    }

    /**
     * 创建新SOP
     * @param userId 所有者用户ID
     * @param req SOP创建请求
     * @return 创建的SOP实体（草稿状态）
     */
    @Override
    @Transactional
    public Sop create(Long userId, SopRequest req) {
        Sop sop = new Sop();
        sop.setUserId(userId);
        fillSop(sop, req);
        sop.setVersion(1);
        sop.setStatus("draft");
        sopMapper.insert(sop);

        createVersionSnapshot(sop, userId, "初始版本");
        return sop;
    }

    /**
     * 更新SOP内容
     * @param id SOP ID
     * @param userId 用户ID，用于权限校验
     * @param req SOP更新请求
     */
    @Override
    @Transactional
    public void update(Long id, Long userId, SopRequest req) {
        Sop sop = getById(id, userId);
        fillSop(sop, req);
        sopMapper.updateById(sop);
    }

    /**
     * 删除SOP及其所有关联数据
     * 包括执行记录、版本快照、定时任务、草稿、异常记录等
     * @param id SOP ID
     * @param userId 用户ID，用于权限校验
     */
    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        Sop sop = getById(id, userId);

        // 1. Delete execution step records + exceptions (via execution ids)
        List<Long> executionIds = executionMapper.selectList(
                new LambdaQueryWrapper<SopExecution>().eq(SopExecution::getSopId, id)
        ).stream().map(SopExecution::getId).collect(java.util.stream.Collectors.toList());
        if (!executionIds.isEmpty()) {
            stepRecordMapper.delete(
                    new LambdaQueryWrapper<ExecutionStepRecord>().in(ExecutionStepRecord::getExecutionId, executionIds)
            );
            exceptionMapper.delete(
                    new LambdaQueryWrapper<SopException>().in(SopException::getExecutionId, executionIds)
            );
        }

        // 2. Delete related records (direct sopId FK)
        executionMapper.delete(new LambdaQueryWrapper<SopExecution>().eq(SopExecution::getSopId, id));
        executionStatMapper.delete(new LambdaQueryWrapper<ExecutionStat>().eq(ExecutionStat::getSopId, id));
        versionMapper.delete(new LambdaQueryWrapper<SopVersion>().eq(SopVersion::getSopId, id));
        scheduleTaskMapper.delete(new LambdaQueryWrapper<ScheduleTask>().eq(ScheduleTask::getSopId, id));
        draftMapper.delete(new LambdaQueryWrapper<SopDraft>().eq(SopDraft::getSopId, id));

        // 3. Finally delete the SOP itself
        sopMapper.deleteById(id);
    }

    /**
     * 发布SOP
     * @param id SOP ID
     * @param userId 用户ID，用于权限校验
     */
    @Override
    @Transactional
    public void publish(Long id, Long userId) {
        publish(id, userId, "发布版本");
    }

    /**
     * 发布SOP并记录变更说明
     * @param id SOP ID
     * @param userId 用户ID
     * @param changeSummary 变更说明
     */
    @Override
    @Transactional
    public void publish(Long id, Long userId, String changeSummary) {
        Sop sop = getById(id, userId);
        sop.setStatus("published");
        sop.setPublishedAt(LocalDateTime.now());
        sop.setVersion(sop.getVersion() == null ? 1 : sop.getVersion() + 1);
        sopMapper.updateById(sop);

        createVersionSnapshot(sop, userId, changeSummary != null && !changeSummary.isBlank() ? changeSummary : "发布版本");
    }

    /**
     * 创建版本快照
     * 将当前SOP内容保存为新版本，旧版本标记为非当前
     */
    private void createVersionSnapshot(Sop sop, Long userId, String summary) {
        // Mark all existing versions as non-current
        versionMapper.selectList(
            new LambdaQueryWrapper<SopVersion>()
                .eq(SopVersion::getSopId, sop.getId())
                .eq(SopVersion::getIsCurrent, 1)
        ).forEach(v -> { v.setIsCurrent(0); versionMapper.updateById(v); });

        // Create new version snapshot
        SopVersion nv = new SopVersion();
        nv.setSopId(sop.getId());
        nv.setVersion(sop.getVersion());
        nv.setChangeSummary(summary);
        nv.setContent(sop.getContent());
        nv.setIsCurrent(1);
        nv.setCreatorId(userId);
        nv.setCreatedAt(LocalDateTime.now());
        versionMapper.insert(nv);
    }

    /**
     * 填充SOP字段
     * 将请求中的标题、描述、分类、内容、标签等写入SOP实体
     */
    private void fillSop(Sop sop, SopRequest req) {
        sop.setTitle(req.getTitle());
        sop.setDescription(req.getDescription());
        sop.setCategory(req.getCategory() != null ? req.getCategory() : "daily");
        try {
            sop.setContent(objectMapper.writeValueAsString(req.getContent()));
            sop.setTags(objectMapper.writeValueAsString(req.getTags()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化失败");
        }
        if (req.getStatus() != null) {
            sop.setStatus(req.getStatus());
        }
    }
}
