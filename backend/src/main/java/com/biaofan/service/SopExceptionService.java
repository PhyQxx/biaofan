package com.biaofan.service;

import com.biaofan.entity.SopException;
import java.util.List;

public interface SopExceptionService {
    /** 上报执行异常 */
    SopException reportException(Long reporterId, Long executionId, String type, String description);
    /** 获取异常记录列表 */
    List<SopException> getExceptions(String status);
    /** H-05: 获取指定用户的异常记录 */
    List<SopException> getExceptionsByUser(Long userId, String status);
    /** 标记异常已处理 */
    void resolve(Long id, Long resolvedBy, String resolution);
}
