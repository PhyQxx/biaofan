package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.*;
import com.biaofan.mapper.*;
import com.biaofan.service.SopVersionService;
import com.biaofan.dto.SopDiff;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * SOP版本服务实现类
 * 管理SOP的版本历史，支持版本查询和回滚操作
 */
@Service
@RequiredArgsConstructor
public class SopVersionServiceImpl implements SopVersionService {

    private final SopVersionMapper versionMapper;
    private final SopMapper sopMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<SopVersion> getVersions(Long sopId) {
        return versionMapper.selectList(
            new LambdaQueryWrapper<SopVersion>()
                .eq(SopVersion::getSopId, sopId)
                .orderByDesc(SopVersion::getVersion)
        );
    }

    @Override
    public SopVersion getVersion(Long sopId, Integer version) {
        SopVersion v = versionMapper.selectOne(
            new LambdaQueryWrapper<SopVersion>()
                .eq(SopVersion::getSopId, sopId)
                .eq(SopVersion::getVersion, version)
        );
        if (v == null) throw new RuntimeException("版本不存在");
        return v;
    }

    @Override
    @Transactional
    public void rollback(Long sopId, Long userId, Integer targetVersion) {
        Sop sop = sopMapper.selectById(sopId);
        if (sop == null) throw new RuntimeException("SOP不存在");
        SopVersion target = getVersion(sopId, targetVersion);

        saveVersionSnapshot(sop, userId, "回滚前快照");

        sop.setContent(target.getContent());
        sop.setVersion(target.getVersion() + 1);
        sopMapper.updateById(sop);

        saveVersionSnapshot(sop, userId, "回滚至v" + targetVersion);
    }

    @Override
    public SopDiff getDiff(Long sopId, Integer v1, Integer v2) {
        SopVersion oldVer = getVersion(sopId, v1);
        SopVersion newVer = getVersion(sopId, v2);

        try {
            JsonNode oldSteps = objectMapper.readTree(oldVer.getContent());
            JsonNode newSteps = objectMapper.readTree(newVer.getContent());

            List<SopDiff.StepDiff> stepDiffs = new ArrayList<>();
            
            int maxLen = Math.max(oldSteps.size(), newSteps.size());
            for (int i = 0; i < maxLen; i++) {
                JsonNode oldStep = i < oldSteps.size() ? oldSteps.get(i) : null;
                JsonNode newStep = i < newSteps.size() ? newSteps.get(i) : null;

                if (oldStep == null) {
                    stepDiffs.add(SopDiff.StepDiff.builder()
                            .type("added")
                            .newIndex(i)
                            .title(new SopDiff.PropertyDiff(null, newStep.get("title").asText(), true))
                            .description(new SopDiff.PropertyDiff(null, newStep.get("description").asText(), true))
                            .build());
                } else if (newStep == null) {
                    stepDiffs.add(SopDiff.StepDiff.builder()
                            .type("removed")
                            .oldIndex(i)
                            .title(new SopDiff.PropertyDiff(oldStep.get("title").asText(), null, true))
                            .description(new SopDiff.PropertyDiff(oldStep.get("description").asText(), null, true))
                            .build());
                } else {
                    boolean titleChanged = !oldStep.get("title").asText().equals(newStep.get("title").asText());
                    boolean descChanged = !oldStep.get("description").asText().equals(newStep.get("description").asText());
                    
                    String type = (titleChanged || descChanged) ? "modified" : "unchanged";
                    
                    stepDiffs.add(SopDiff.StepDiff.builder()
                            .type(type)
                            .oldIndex(i)
                            .newIndex(i)
                            .title(new SopDiff.PropertyDiff(oldStep.get("title").asText(), newStep.get("title").asText(), titleChanged))
                            .description(new SopDiff.PropertyDiff(oldStep.get("description").asText(), newStep.get("description").asText(), descChanged))
                            .build());
                }
            }

            return SopDiff.builder()
                    .v1(v1)
                    .v2(v2)
                    .steps(stepDiffs)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Diff 解析失败: " + e.getMessage());
        }
    }

    public void saveVersionSnapshot(Sop sop, Long userId, String summary) {
        versionMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SopVersion>()
                .eq(SopVersion::getSopId, sop.getId())
                .eq(SopVersion::getIsCurrent, 1)
                .set(SopVersion::getIsCurrent, 0)
        );

        SopVersion v = new SopVersion();
        v.setSopId(sop.getId());
        v.setVersion(sop.getVersion());
        v.setContent(sop.getContent());
        v.setChangeSummary(summary);
        v.setIsCurrent(1);
        v.setCreatorId(userId);
        versionMapper.insert(v);
    }
}
