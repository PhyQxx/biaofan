package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.Sop;
import com.biaofan.entity.SopInstance;
import com.biaofan.service.SopInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SOP实例控制器
 * <p>管理SOP实例的生命周期，包括查看实例列表、完成步骤、激活、结束实例等操作。
 * 与ExecutionController的区别在于：Instance更侧重被分发的SOP执行实例管理。</p>
 *
 * @RestController
 * @RequestMapping("/api/instance")
 * @RequiredArgsConstructor
 */
@RestController
@RequestMapping("/api/instance")
@RequiredArgsConstructor
public class SopInstanceController {

    private final SopInstanceService instanceService;

    /**
     * 获取当前用户的SOP实例列表
     *
     * @param userId 当前登录用户ID
     * @param status 可选的实例状态过滤
     * @return 实例列表
     */
    @GetMapping("/my")
    public Result<List<SopInstance>> myInstances(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String status) {
        return Result.ok(instanceService.getMyInstances(userId, status));
    }

    /**
     * 获取指定实例详情
     *
     * @param userId 当前登录用户ID
     * @param id     实例ID
     * @return 实例详情及其关联的SOP信息
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getInstance(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        SopInstance inst = instanceService.getInstance(id);
        Sop sop = instanceService.getSopByInstanceId(id);
        return Result.ok(Map.of(
                "instance", inst,
                "sop", sop
        ));
    }

    /**
     * 激活SOP实例
     * <p>将实例状态从待激活恢复为进行中。</p>
     *
     * @param userId 当前登录用户ID
     * @param id     实例ID
     * @return 操作结果
     */
    @PostMapping("/{id}/activate")
    public Result<Void> activate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        try {
            instanceService.activateInstance(userId, id);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 完成指定步骤
     * <p>记录用户在实例中完成某个步骤，支持填写备注和检查数据。</p>
     *
     * @param userId     当前登录用户ID
     * @param id         实例ID
     * @param stepIndex  步骤索引（从0开始）
     * @param body       可选请求体，包含notes（备注）和checkData（检查数据）
     * @return 完成结果
     */
    @PostMapping("/{id}/steps/{stepIndex}/complete")
    public Result<Map<String, Object>> completeStep(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @PathVariable int stepIndex,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String notes = body != null && body.get("notes") != null ? body.get("notes").toString() : null;
            @SuppressWarnings("unchecked")
            Map<String, Object> checkData = body != null && body.get("checkData") != null
                    ? (Map<String, Object>) body.get("checkData") : null;
            boolean completed = instanceService.completeStep(userId, id, stepIndex, notes, checkData);
            return Result.ok(Map.of("completed", completed));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 结束实例
     * <p>标记实例为已完成状态。</p>
     *
     * @param userId 当前登录用户ID
     * @param id     实例ID
     * @return 操作结果
     */
    @PostMapping("/{id}/finish")
    public Result<Void> finish(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        try {
            instanceService.finishInstance(userId, id);
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
