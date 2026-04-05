package com.biaofan.service;

import com.biaofan.entity.SopException;
import java.util.List;

public interface SopExceptionService {
    /** 上报执行异常 */
    SopException reportException(Long reporterId, Long executionId, String type, String description);
    /** 获取异常记录列表 */
    List<SopException> getExceptions(String status);
    /** 标记异常已处理 */
    void resolve(Long id, Long resolvedBy, String resolution);
}
