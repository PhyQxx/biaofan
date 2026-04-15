package com.biaofan.service;

import com.biaofan.entity.Sop;
import com.biaofan.entity.SopInstance;
import java.util.List;

/**
 * SOP实例服务接口
 * 提供SOP实例的管理，包括周期性实例生成、逾期检测等功能
 */
public interface SopInstanceService {
    /**
     * 获取当前用户的SOP实例列表
     * @param userId 用户ID
     * @param status 实例状态（可为空，查全部）
     * @return 实例列表
     */
    List<SopInstance> getMyInstances(Long userId, String status);

    /**
     * 获取SOP实例详情
     * @param instanceId 实例ID
     * @return 实例实体
     */
    SopInstance getInstance(Long instanceId);

    /**
     * 根据实例ID获取关联的SOP信息
     * @param instanceId 实例ID
     * @return SOP实体
     */
    Sop getSopByInstanceId(Long instanceId);

    /**
     * 激活/重新开始一个SOP实例
     * @param userId 用户ID
     * @param instanceId 实例ID
     */
    void activateInstance(Long userId, Long instanceId);

    /**
     * 完成SOP实例的某个步骤
     * @param userId 用户ID
     * @param instanceId 实例ID
     * @param stepIndex 步骤索引（从0开始）
     * @param notes 步骤备注
     * @param checkData 步骤检查数据
     * @return 是否成功完成
     */
    boolean completeStep(Long userId, Long instanceId, int stepIndex, String notes, java.util.Map<String, Object> checkData);

    /**
     * 结束SOP实例
     * @param userId 用户ID
     * @param instanceId 实例ID
     */
    void finishInstance(Long userId, Long instanceId);

    /**
     * 生成周期性的SOP实例（定时任务调用）
     * 根据配置的cron表达式，自动为用户生成应执行的SOP实例
     */
    void generatePeriodicInstances();

    /**
     * 标记逾期未完成的SOP实例
     * 检查实例是否超过截止时间，标记为逾期状态
     */
    void markOverdueInstances();
}
