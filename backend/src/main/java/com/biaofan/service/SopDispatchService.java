package com.biaofan.service;

import com.biaofan.entity.SopDispatch;
import java.util.List;

public interface SopDispatchService {
    /** 批量分发 SOP 给多个用户 */
    List<SopDispatch> batchDispatch(Long dispatcherId, List<Long> templateIds, List<Long> assigneeIds);
    /** 获取当前用户的分发记录 */
    List<SopDispatch> getMyDispatches(Long userId);
}
