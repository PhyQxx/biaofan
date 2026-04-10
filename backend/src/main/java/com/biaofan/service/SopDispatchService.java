package com.biaofan.service;

import com.biaofan.entity.SopDispatch;
import java.util.List;

/**
 * SOP分发服务接口
 * 提供将SOP模板批量分发给多个用户的功能
 */
public interface SopDispatchService {
    /**
     * 批量分发SOP给多个用户
     * @param dispatcherId 分发者用户ID
     * @param templateIds 要分发的SOP模板ID列表
     * @param assigneeIds 被分发用户ID列表
     * @return 分发记录列表
     */
    List<SopDispatch> batchDispatch(Long dispatcherId, List<Long> templateIds, List<Long> assigneeIds);

    /**
     * 获取当前用户的分发记录
     * 包括：我分发给别人的记录，以及别人分发给我的记录
     * @param userId 用户ID
     * @return 分发记录列表
     */
    List<SopDispatch> getMyDispatches(Long userId);
}
