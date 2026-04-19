package com.biaofan.service;

import com.biaofan.entity.ExecutionStepRecord;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopExecution;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * SOP执行服务接口
 * 提供SOP执行的全流程管理，包括开始执行、完成步骤、结束执行等功能
 */
public interface SopExecutionService {
    /**
     * 开始执行一个SOP
     * @param userId 执行者用户ID
     * @param sopId SOP ID
     * @return 创建的执行记录
     */
    SopExecution startExecution(Long userId, Long sopId);

    /**
     * 完成SOP执行中的某个步骤
     * @param userId 执行者用户ID
     * @param executionId 执行记录ID
     * @param stepIndex 步骤索引（从0开始）
     * @param notes 步骤备注/说明
     * @param checkData 步骤检查数据（如表单数据）
     * @return 是否成功完成
     */
    boolean completeStep(Long userId, Long executionId, int stepIndex, String notes,
                         Map<String, Object> checkData);

    /**
     * 结束SOP执行
     * @param userId 执行者用户ID
     * @param executionId 执行记录ID
     */
    void finishExecution(Long userId, Long executionId);

    /**
     * 激活/重新开始一个SOP执行
     * @param userId 执行者用户ID
     * @param executionId 执行记录ID
     */
    void activateExecution(Long userId, Long executionId);

    /**
     * 获取当前用户的执行记录列表
     * @param userId 用户ID
     * @param status 执行状态（可为空，查全部）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 执行记录分页列表
     */
    Page<SopExecution> getMyExecutions(Long userId, String status, int page, int pageSize);

    /**
     * 获取执行记录详情
     * @param executionId 执行记录ID
     * @return 执行记录实体
     */
    SopExecution getExecution(Long executionId);

    /**
     * 获取SOP详情（包含所有步骤）
     * @param sopId SOP ID
     * @return SOP实体（含步骤列表）
     */
    Sop getSopWithSteps(Long sopId);

    /**
     * 获取执行记录的步骤详情列表
     * @param executionId 执行记录ID
     * @return 步骤执行记录列表
     */
    List<ExecutionStepRecord> getStepRecords(Long executionId);

    /**
     * 获取执行记录的总步骤数
     * @param executionId 执行记录ID
     * @return 步骤数量
     */
    int getStepCount(Long executionId);
}
