package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.*;
import com.biaofan.mapper.*;
import com.biaofan.service.SopVersionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SOP版本服务实现类
 * 管理SOP的版本历史，支持版本查询和回滚操作
 * 每次回滚会创建快照记录，确保版本可追溯
 *
 * @author biaofan
 */
@Service
@RequiredArgsConstructor
public class SopVersionServiceImpl implements SopVersionService {

    private final SopVersionMapper versionMapper;
    private final SopMapper sopMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取SOP的所有历史版本
     * @param sopId SOP ID
     * @return 版本列表，按版本号倒序
     */
    @Override
    public List<SopVersion> getVersions(Long sopId) {
        return versionMapper.selectList(
            new LambdaQueryWrapper<SopVersion>()
                .eq(SopVersion::getSopId, sopId)
                .orderByDesc(SopVersion::getVersion)
        );
    }

    /**
     * 获取指定版本的详情
     * @param sopId SOP ID
     * @param version 版本号
     * @return 版本详情
     */
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

    /**
     * 回滚SOP到指定版本
     * 回滚前先快照当前版本，回滚后创建新版本记录
     * @param sopId SOP ID
     * @param userId 操作人用户ID
     * @param targetVersion 目标版本号
     */
    @Override
    @Transactional
    public void rollback(Long sopId, Long userId, Integer targetVersion) {
        Sop sop = sopMapper.selectById(sopId);
        if (sop == null) throw new RuntimeException("SOP不存在");
        SopVersion target = getVersion(sopId, targetVersion);

        // 快照当前版本
        saveVersionSnapshot(sop, userId, "回滚前快照");

        // 回滚到目标版本
        sop.setContent(target.getContent());
        sop.setVersion(target.getVersion() + 1);
        sopMapper.updateById(sop);

        // 记录新版本
        saveVersionSnapshot(sop, userId, "回滚至v" + targetVersion);
    }

    /**
     * 保存版本快照
     * @param sop SOP实体
     * @param userId 创建者ID
     * @param summary 变更说明
     */
    public void saveVersionSnapshot(Sop sop, Long userId, String summary) {
        // 清除旧当前版本标记
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
