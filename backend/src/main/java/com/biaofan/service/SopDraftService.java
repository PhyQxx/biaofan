package com.biaofan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.SopDraft;
import com.biaofan.mapper.SopDraftMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SopDraftService {

    private final SopDraftMapper draftMapper;

    /**
     * 同步离线草稿：如果存在则更新，不存在则创建
     * @return {draftId, syncedAt}
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
