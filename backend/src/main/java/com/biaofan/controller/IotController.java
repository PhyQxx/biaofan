package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.SopExecution;
import com.biaofan.mapper.SopExecutionMapper;
import com.biaofan.service.SopExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * IoT 设备对接控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
public class IotController {

    private final SopExecutionService executionService;
    private final SopExecutionMapper executionMapper;

    /**
     * 接收 IoT 设备数据并尝试自动触发步骤
     * POST /api/iot/webhook
     */
    @PostMapping("/webhook")
    public Result<Void> handleIotData(@RequestBody Map<String, Object> data) {
        String deviceId = (String) data.get("deviceId");
        if (deviceId == null) return Result.fail("deviceId 不能为空");
        
        log.info("[IoT] Received data from device: {}", deviceId);

        // 1. 查找所有正在进行的且当前步骤可能与该设备关联的执行单
        // 简化逻辑：只处理 in_progress 的单子
        List<SopExecution> activeExecs = executionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SopExecution>()
                .eq(SopExecution::getStatus, "in_progress")
        );

        for (SopExecution exec : activeExecs) {
            try {
                // TODO: 在 Service 中实现匹配逻辑，检查当前步骤是否为 IoT 且配置匹配
                // 暂时记录日志
                log.debug("Checking IoT match for execution {} and device {}", exec.getId(), deviceId);
                
                // 模拟自动完成 (假设匹配成功)
                // executionService.completeStep(exec.getExecutorId(), exec.getId(), exec.getCurrentStep(), "IoT 自动触发", data, null, null);
            } catch (Exception e) {
                log.error("Failed to process IoT data for execution {}", exec.getId(), e);
            }
        }

        return Result.ok();
    }
}
