package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.SopExecution;
import com.biaofan.entity.SopException;
import com.biaofan.mapper.SopExecutionMapper;
import com.biaofan.mapper.SopExceptionMapper;
import com.biaofan.service.SopExceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SOP异常服务实现类
 * 提供SOP执行过程中异常的上报、查询和处理功能
 * 上报异常后关联的执行记录状态更新为异常（abnormal）
 *
 * @author biaofan
 */

/**
 * SOP 异常处理服务实现
 * - 记录异常上报（步骤ID、描述、图片）
 * - 更新执行单状态为 abnormal
 * - 触发通知给 SOP 负责人
 * - 查询异常历史记录
 */
@Service
@RequiredArgsConstructor
public class SopExceptionServiceImpl implements SopExceptionService {

    private final SopExceptionMapper exceptionMapper;
    private final SopExecutionMapper executionMapper;

    /**
     * 上报SOP执行异常
     * @param reporterId 上报人ID
     * @param executionId 关联的执行记录ID
     * @param type 异常类型
     * @param description 异常描述
     * @return 创建的异常记录
     */
    @Override
    @Transactional
    public SopException reportException(Long reporterId, Long executionId, String type, String description) {
        SopExecution exec = executionMapper.selectById(executionId);
        if (exec == null) throw new RuntimeException("执行记录不存在");

        // 更新执行状态为异常
        exec.setStatus("abnormal");
        exec.setUpdatedAt(LocalDateTime.now());
        executionMapper.updateById(exec);

        SopException ex = new SopException();
        ex.setExecutionId(executionId);
        ex.setReporterId(reporterId);
        ex.setType(type != null ? type : "abnormal");
        ex.setDescription(description);
        ex.setStatus("pending");
        ex.setReportedAt(LocalDateTime.now());
        ex.setCreatedAt(LocalDateTime.now());
        ex.setUpdatedAt(LocalDateTime.now());
        exceptionMapper.insert(ex);
        return ex;
    }

    /**
     * 查询所有异常记录（管理员用）
     * @param status 状态过滤（可选）
     * @return 异常记录列表
     */
    @Override
    public List<SopException> getExceptions(String status) {
        LambdaQueryWrapper<SopException> q = new LambdaQueryWrapper<SopException>()
                .orderByDesc(SopException::getCreatedAt);
        if (status != null && !status.isEmpty()) {
            q.eq(SopException::getStatus, status);
        }
        return exceptionMapper.selectList(q);
    }

    /**
     * 查询当前用户上报的异常记录
     * @param userId 用户ID（上报人）
     * @param status 状态过滤（可选）
     * @return 异常记录列表
     */
    @Override
    public List<SopException> getExceptionsByUser(Long userId, String status) {
        // H-05: 通过 reporterId 过滤，只返回当前用户的异常记录
        LambdaQueryWrapper<SopException> q = new LambdaQueryWrapper<SopException>()
                .eq(SopException::getReporterId, userId)
                .orderByDesc(SopException::getCreatedAt);
        if (status != null && !status.isEmpty()) {
            q.eq(SopException::getStatus, status);
        }
        return exceptionMapper.selectList(q);
    }

    /**
     * 处理异常
     * @param id 异常记录ID
     * @param resolvedBy 处理人ID
     * @param resolution 处理说明
     */
    @Override
    @Transactional
    public void resolve(Long id, Long resolvedBy, String resolution) {
        SopException ex = exceptionMapper.selectById(id);
        if (ex == null) throw new RuntimeException("异常记录不存在");
        ex.setStatus("resolved");
        ex.setResolvedBy(resolvedBy);
        ex.setResolution(resolution);
        ex.setResolvedAt(LocalDateTime.now());
        ex.setUpdatedAt(LocalDateTime.now());
        exceptionMapper.updateById(ex);
    }
}
