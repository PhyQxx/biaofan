package com.biaofan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.biaofan.entity.Sop;
import com.biaofan.dto.SopRequest;

/**
 * SOP（标准操作程序）服务接口
 * 提供SOP的创建、编辑、发布、删除等生命周期管理功能
 */
public interface SopService {
    /**
     * 获取当前用户的SOP列表（分页）
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return SOP分页列表
     */
    IPage<Sop> getMySops(Long userId, int page, int size);

    /**
     * 根据ID获取SOP详情
     * @param id SOP ID
     * @param userId 当前用户ID（用于权限校验）
     * @return SOP实体
     */
    Sop getById(Long id, Long userId);

    /**
     * 创建新的SOP
     * @param userId 创建者用户ID
     * @param req SOP创建请求（包含标题、步骤等内容）
     * @return 创建的SOP实体
     */
    Sop create(Long userId, SopRequest req);

    /**
     * 更新SOP内容
     * @param id SOP ID
     * @param userId 当前用户ID
     * @param req SOP更新请求
     */
    void update(Long id, Long userId, SopRequest req);

    /**
     * 删除SOP（仅创建者可删除）
     * @param id SOP ID
     * @param userId 当前用户ID
     */
    void delete(Long id, Long userId);

    /**
     * 发布SOP
     * @param id SOP ID
     * @param userId 发布者用户ID
     */
    void publish(Long id, Long userId);

    /**
     * 发布SOP（带变更摘要）
     * @param id SOP ID
     * @param userId 发布者用户ID
     * @param changeSummary 本次发布的变更说明
     */
    void publish(Long id, Long userId, String changeSummary);
}
