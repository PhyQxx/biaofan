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

@Service
@RequiredArgsConstructor
public class SopExceptionServiceImpl implements SopExceptionService {

    private final SopExceptionMapper exceptionMapper;
    private final SopExecutionMapper executionMapper;

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

    @Override
    public List<SopException> getExceptions(String status) {
        LambdaQueryWrapper<SopException> q = new LambdaQueryWrapper<SopException>()
                .orderByDesc(SopException::getCreatedAt);
        if (status != null && !status.isEmpty()) {
            q.eq(SopException::getStatus, status);
        }
        return exceptionMapper.selectList(q);
    }

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
