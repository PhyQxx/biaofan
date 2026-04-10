package com.biaofan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.SopDraft;
import com.biaofan.mapper.SopDraftMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * SOP草稿同步服务
 * 负责离线草稿数据的同步管理，支持执行过程中断后的草稿恢复
 */
@Service
@RequiredArgsConstructor
public class SopDraftService {

    private final SopDraftMapper draftMapper;

    /**
     * 同步离线草稿：如果存在则更新，不存在则创建
     * 用于客户端离线填写后同步到服务端，支持断点续传场景
     * @param executorId 执行者用户ID
     * @param sopId SOP ID
     * @param draftData 草稿数据（JSON格式）
     * @param clientUpdatedAt 客户端最后更新时间戳
     * @return 同步后的草稿实体
     * @return draftId 草稿ID
     * @return syncedAt 同步时间
     */
    public SopDraft syncDraft(Long executorId, Long sopId, String draftData, Long clientUpdatedAt) {
        SopDraft existing = draftMapper.findByExecutorAndSop(executorId, sopId);
        LocalDateTime now = LocalDateTime.now();
        long syncedAt = System.currentTimeMillis();

        if (existing != null) {
            existing.setDraftData(draftData);
            existing.setClientUpdatedAt(clientUpdatedAt);
            existing.setSyncedAt(now);
            draftMapper.updateById(existing);
            return existing;
        } else {
            SopDraft draft = new SopDraft();
            draft.setExecutorId(executorId);
            draft.setSopId(sopId);
            draft.setDraftData(draftData);
            draft.setClientUpdatedAt(clientUpdatedAt);
            draft.setSyncedAt(now);
            draft.setCreatedAt(now);
            draft.setUpdatedAt(now);
            draftMapper.insert(draft);
            return draft;
        }
    }
}
