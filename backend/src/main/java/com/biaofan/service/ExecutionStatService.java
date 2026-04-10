package com.biaofan.service;

import com.biaofan.entity.ExecutionStat;
import java.util.List;

/**
 * 执行统计数据服务接口
 * 提供SOP执行相关的数据统计分析功能
 */
public interface ExecutionStatService {
    /**
     * 获取当前用户的执行统计数据
     * @param userId 用户ID
     * @return 统计数据列表
     */
    List<ExecutionStat> getStats(Long userId);

    /**
     * 获取全局执行统计数据（所有用户）
     * @return 统计数据列表
     */
    List<ExecutionStat> getGlobalStats();
}
