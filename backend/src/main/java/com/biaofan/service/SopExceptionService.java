package com.biaofan.service;

import com.biaofan.entity.SopException;
import java.util.List;

/**
 * SOP异常服务接口
 * 提供SOP执行异常的上报、查询和处理功能
 */
public interface SopExceptionService {
    /**
     * 上报执行异常
     * @param reporterId 上报人用户ID
     * @param executionId 相关的执行记录ID
     * @param type 异常类型
     * @param description 异常描述
     * @return 创建的异常记录
     */
    SopException reportException(Long reporterId, Long executionId, String type, String description);

    /**
     * 获取异常记录列表
     * @param status 异常状态（如待处理、已处理，可为空查全部）
     * @return 异常记录列表
     */
    List<SopException> getExceptions(String status);

    /**
     * 获取指定用户的异常记录
     * @param userId 用户ID
     * @param status 异常状态（可为空查全部）
     * @return 异常记录列表
     */
    List<SopException> getExceptionsByUser(Long userId, String status);

    /**
     * 标记异常已处理
     * @param id 异常记录ID
     * @param resolvedBy 处理人用户ID
     * @param resolution 处理说明/解决方案
     */
    void resolve(Long id, Long resolvedBy, String resolution);
}
