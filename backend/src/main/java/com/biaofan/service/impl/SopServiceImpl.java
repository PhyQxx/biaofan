package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.*;
import com.biaofan.dto.SopRequest;
import com.biaofan.mapper.*;
import com.biaofan.service.SopService;
import com.biaofan.service.OrganizationService;
import com.biaofan.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SOP（标准操作程序）服务实现类
 * 提供SOP的创建、更新、删除、发布及审核管理
 */
@Slf4j
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
    private final SopApprovalRecordMapper approvalRecordMapper;
    private final OrganizationService organizationService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<Sop> getMySops(Long userId, Long orgId, int page, int size) {
        Page<Sop> p = new Page<>(page, size);
        LambdaQueryWrapper<Sop> q = new LambdaQueryWrapper<Sop>()
                .orderByDesc(Sop::getUpdatedAt);
                
        if (orgId == null) {
            q.eq(Sop::getUserId, userId);
            q.isNull(Sop::getOrgId);
        } else {
            q.eq(Sop::getOrgId, orgId);
        }
        
        return sopMapper.selectPage(p, q);
    }

    @Override
    public Sop getById(Long id, Long userId) {
        Sop sop = sopMapper.selectById(id);
        if (sop == null) throw new RuntimeException("SOP不存在");
        if (sop.getOrgId() == null && !sop.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该个人 SOP");
        }
        return sop;
    }

    @Override
    @Transactional
    public Sop create(Long userId, Long orgId, SopRequest req) {
        Sop sop = new Sop();
        sop.setUserId(userId);
        sop.setOrgId(orgId);
        fillSop(sop, req);
        sop.setVersion(1);
        sop.setStatus("draft");
        sopMapper.insert(sop);

        createVersionSnapshot(sop, userId, "初始版本");
        return sop;
    }

    @Override
    @Transactional
    public void update(Long id, Long userId, SopRequest req) {
        Sop sop = getById(id, userId);
        if ("pending_review".equals(sop.getStatus())) {
            throw new RuntimeException("处于审核中的 SOP 不允许修改");
        }
        fillSop(sop, req);
        sopMapper.updateById(sop);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        Sop sop = getById(id, userId);

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

        executionMapper.delete(new LambdaQueryWrapper<SopExecution>().eq(SopExecution::getSopId, id));
        executionStatMapper.delete(new LambdaQueryWrapper<ExecutionStat>().eq(ExecutionStat::getSopId, id));
        versionMapper.delete(new LambdaQueryWrapper<SopVersion>().eq(SopVersion::getSopId, id));
        scheduleTaskMapper.delete(new LambdaQueryWrapper<ScheduleTask>().eq(ScheduleTask::getSopId, id));
        draftMapper.delete(new LambdaQueryWrapper<SopDraft>().eq(SopDraft::getSopId, id));

        sopMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void publish(Long id, Long userId) {
        publish(id, userId, "发布版本");
    }

    @Override
    @Transactional
    public void publish(Long id, Long userId, String changeSummary) {
        Sop sop = getById(id, userId);
        
        if (sop.getOrgId() != null) {
            throw new RuntimeException("组织 SOP 必须经过审核后发布");
        }

        sop.setStatus("published");
        sop.setPublishedAt(LocalDateTime.now());
        sop.setVersion(sop.getVersion() == null ? 1 : sop.getVersion() + 1);
        sopMapper.updateById(sop);

        createVersionSnapshot(sop, userId, changeSummary != null && !changeSummary.isBlank() ? changeSummary : "发布版本");
    }

    @Override
    @Transactional
    public void submitForReview(Long id, Long userId) {
        Sop sop = sopMapper.selectById(id);
        if (sop == null) throw new RuntimeException("SOP不存在");
        if (!sop.getUserId().equals(userId)) throw new RuntimeException("仅所有者可提交审核");
        if (sop.getOrgId() == null) throw new RuntimeException("个人 SOP 无需审核");
        
        sop.setStatus("pending_review");
        sop.setUpdatedAt(LocalDateTime.now());
        sopMapper.updateById(sop);
        
        SopApprovalRecord record = new SopApprovalRecord();
        record.setSopId(id);
        record.setOrgId(sop.getOrgId());
        record.setSubmitterId(userId);
        record.setStatus("pending");
        record.setCreatedAt(LocalDateTime.now());
        approvalRecordMapper.insert(record);

        // 通知组织管理员
        List<Long> admins = organizationService.listAdmins(sop.getOrgId());
        for (Long adminId : admins) {
            notificationService.createAndDispatch(
                adminId,
                "sop_review_requested",
                "SOP 审核申请",
                "成员提交了新的 SOP《" + sop.getTitle() + "》，请及时审核。",
                "sop",
                id
            );
        }
    }

    @Override
    @Transactional
    public void approve(Long id, Long reviewerId, String comment) {
        Sop sop = sopMapper.selectById(id);
        if (sop == null) throw new RuntimeException("SOP不存在");
        if (!"pending_review".equals(sop.getStatus())) throw new RuntimeException("SOP不在待审核状态");
        
        if (!organizationService.isMember(reviewerId, sop.getOrgId())) {
            throw new RuntimeException("无权审核该组织的 SOP");
        }
        
        sop.setStatus("published");
        sop.setPublishedAt(LocalDateTime.now());
        sop.setUpdatedAt(LocalDateTime.now());
        sop.setVersion(sop.getVersion() == null ? 1 : sop.getVersion() + 1);
        sopMapper.updateById(sop);
        
        createVersionSnapshot(sop, reviewerId, "审核通过并自动发布");

        SopApprovalRecord record = approvalRecordMapper.selectOne(
            new LambdaQueryWrapper<SopApprovalRecord>()
                .eq(SopApprovalRecord::getSopId, id)
                .eq(SopApprovalRecord::getStatus, "pending")
                .orderByDesc(SopApprovalRecord::getCreatedAt)
                .last("LIMIT 1")
        );
        if (record != null) {
            record.setReviewerId(reviewerId);
            record.setStatus("approved");
            record.setComment(comment);
            record.setReviewedAt(LocalDateTime.now());
            approvalRecordMapper.updateById(record);
        }

        // 通知创建者
        notificationService.createAndDispatch(
            sop.getUserId(),
            "sop_approved",
            "SOP 审核通过",
            "您的 SOP《" + sop.getTitle() + "》已审核通过并发布。",
            "sop",
            id
        );
    }

    @Override
    @Transactional
    public void reject(Long id, Long reviewerId, String comment) {
        Sop sop = sopMapper.selectById(id);
        if (sop == null) throw new RuntimeException("SOP不存在");
        if (!"pending_review".equals(sop.getStatus())) throw new RuntimeException("SOP不在待审核状态");

        if (!organizationService.isMember(reviewerId, sop.getOrgId())) {
            throw new RuntimeException("无权审核该组织的 SOP");
        }

        sop.setStatus("rejected");
        sop.setUpdatedAt(LocalDateTime.now());
        sopMapper.updateById(sop);

        SopApprovalRecord record = approvalRecordMapper.selectOne(
            new LambdaQueryWrapper<SopApprovalRecord>()
                .eq(SopApprovalRecord::getSopId, id)
                .eq(SopApprovalRecord::getStatus, "pending")
                .orderByDesc(SopApprovalRecord::getCreatedAt)
                .last("LIMIT 1")
        );
        if (record != null) {
            record.setReviewerId(reviewerId);
            record.setStatus("rejected");
            record.setComment(comment);
            record.setReviewedAt(LocalDateTime.now());
            approvalRecordMapper.updateById(record);
        }

        // 通知创建者
        notificationService.createAndDispatch(
            sop.getUserId(),
            "sop_rejected",
            "SOP 审核驳回",
            "您的 SOP《" + sop.getTitle() + "》已被驳回。意见：" + (comment != null ? comment : "无"),
            "sop",
            id
        );
    }

    @Override
    @Transactional
    public void transferToOrganization(Long id, Long userId, Long orgId) {
        Sop sop = sopMapper.selectById(id);
        if (sop == null) throw new RuntimeException("SOP不存在");
        if (sop.getOrgId() != null) throw new RuntimeException("该 SOP 已经属于一个组织");
        if (!sop.getUserId().equals(userId)) throw new RuntimeException("仅所有者可转移 SOP");
        
        if (!organizationService.isMember(userId, orgId)) {
            throw new RuntimeException("您不是该组织的成员");
        }

        sop.setOrgId(orgId);
        // 转移后状态改为草稿，需重新通过组织的审核流程
        sop.setStatus("draft");
        sop.setUpdatedAt(LocalDateTime.now());
        sopMapper.updateById(sop);
    }

    private void createVersionSnapshot(Sop sop, Long userId, String summary) {
        versionMapper.update(null, new LambdaUpdateWrapper<SopVersion>()
                .eq(SopVersion::getSopId, sop.getId())
                .set(SopVersion::getIsCurrent, 0));

        SopVersion nv = new SopVersion();
        nv.setSopId(sop.getId());
        nv.setVersion(sop.getVersion());
        nv.setChangeSummary(summary);
        String content = sop.getContent();
        if (content != null && content.length() > 60_000) {
            throw new RuntimeException("SOP内容过大，请精简后重试");
        }
        nv.setContent(content);
        nv.setIsCurrent(1);
        nv.setCreatorId(userId);
        nv.setCreatedAt(LocalDateTime.now());
        versionMapper.insert(nv);
    }

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
    }
}
