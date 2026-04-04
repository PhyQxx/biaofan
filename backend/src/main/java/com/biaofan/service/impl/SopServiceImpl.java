package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopVersion;
import com.biaofan.dto.SopRequest;
import com.biaofan.mapper.SopMapper;
import com.biaofan.mapper.SopVersionMapper;
import com.biaofan.service.SopService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SopServiceImpl implements SopService {

    private final SopMapper sopMapper;
    private final SopVersionMapper versionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public IPage<Sop> getMySops(Long userId, int page, int size) {
        Page<Sop> p = new Page<>(page, size);
        LambdaQueryWrapper<Sop> q = new LambdaQueryWrapper<Sop>()
                .eq(Sop::getUserId, userId)
                .orderByDesc(Sop::getUpdatedAt);
        return sopMapper.selectPage(p, q);
    }

    @Override
    public Sop getById(Long id, Long userId) {
        Sop sop = sopMapper.selectById(id);
        if (sop == null) throw new RuntimeException("SOP不存在");
        if (!sop.getUserId().equals(userId)) throw new RuntimeException("无权访问");
        return sop;
    }

    @Override
    @Transactional
    public void create(Long userId, SopRequest req) {
        Sop sop = new Sop();
        sop.setUserId(userId);
        fillSop(sop, req);
        sop.setVersion(1);
        sop.setStatus("draft");
        sopMapper.insert(sop);

        // Auto-create initial version v1
        createVersionSnapshot(sop, userId, "初始版本");
    }

    @Override
    @Transactional
    public void update(Long id, Long userId, SopRequest req) {
        Sop sop = getById(id, userId);
        fillSop(sop, req);
        sopMapper.updateById(sop);
    }

    @Override
    public void delete(Long id, Long userId) {
        Sop sop = getById(id, userId);
        sopMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void publish(Long id, Long userId) {
        Sop sop = getById(id, userId);
        sop.setStatus("published");
        sop.setPublishedAt(LocalDateTime.now());
        sopMapper.updateById(sop);

        // Create version snapshot on publish
        if (sop.getVersion() == null || sop.getVersion() < 1) {
            sop.setVersion(1);
            sopMapper.updateById(sop);
        }
        createVersionSnapshot(sop, userId, "发布版本");
    }

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

    private void fillSop(Sop sop, SopRequest req) {
        sop.setTitle(req.getTitle());
        sop.setDescription(req.getDescription());
        sop.setCategory(req.getCategory() != null ? req.getCategory() : "其他");
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
