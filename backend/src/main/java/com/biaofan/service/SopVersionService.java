package com.biaofan.service;

import com.biaofan.entity.SopVersion;
import java.util.List;

/**
 * SOP版本服务接口
 * 提供SOP的历史版本查询和回滚功能
 */
public interface SopVersionService {
    /**
     * 获取SOP的所有历史版本
     * @param sopId SOP ID
     * @return 版本列表（按版本号降序）
     */
    List<SopVersion> getVersions(Long sopId);

    /**
     * 获取SOP指定版本号的版本详情
     * @param sopId SOP ID
     * @param version 版本号
     * @return 版本实体
     */
    SopVersion getVersion(Long sopId, Integer version);

    /**
     * 回滚SOP到指定版本
     * @param sopId SOP ID
     * @param userId 操作者用户ID
     * @param targetVersion 目标版本号
     */
    void rollback(Long sopId, Long userId, Integer targetVersion);
}
