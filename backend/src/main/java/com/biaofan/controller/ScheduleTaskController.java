package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.ScheduleTask;
import com.biaofan.service.ScheduleTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule/tasks")
@RequiredArgsConstructor
public class ScheduleTaskController {

    private final ScheduleTaskService scheduleTaskService;

    /**
     * 获取当前用户的定时任务列表
     */
    @GetMapping
    public Result<List<ScheduleTask>> list(@AuthenticationPrincipal Long userId) {
        List<ScheduleTask> tasks = scheduleTaskService.getMyTasks(userId);
        return Result.ok(tasks);
    }

    /**
     * 获取某个 SOP 的定时任务
     */
    @GetMapping("/{sopId}")
    public Result<ScheduleTask> getBySopId(
            @PathVariable Long sopId,
            @AuthenticationPrincipal Long userId) {
        ScheduleTask task = scheduleTaskService.getBySopId(sopId, userId);
        return Result.ok(task);
    }

    /**
     * 创建或更新定时任务
     * POST /api/schedule/tasks
     * body: { sopId, cronExpression }
     */
    @PostMapping
    public Result<Void> save(
            @AuthenticationPrincipal Long userId,
            @RequestBody ScheduleTaskRequest req) {
        try {
            scheduleTaskService.create(req.getSopId(), userId, req.getCronExpression());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 删除定时任务
     * DELETE /api/schedule/tasks/{sopId}
     */
    @DeleteMapping("/{sopId}")
    public Result<Void> delete(
            @PathVariable Long sopId,
            @AuthenticationPrincipal Long userId) {
        scheduleTaskService.delete(sopId, userId);
        return Result.ok();
    }

    /**
     * 启用/停用定时任务
     * PUT /api/schedule/tasks/{id}/enable
     * body: { enabled: 0|1 }
     */
    @PutMapping("/{id}/enable")
    public Result<Void> toggleEnable(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestBody EnableRequest req) {
        scheduleTaskService.updateEnabled(id, userId, req.getEnabled());
        return Result.ok();
    }

    public static class ScheduleTaskRequest {
        private Long sopId;
        private String cronExpression;

        public Long getSopId() { return sopId; }
        public void setSopId(Long sopId) { this.sopId = sopId; }
        public String getCronExpression() { return cronExpression; }
        public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    }

    public static class EnableRequest {
        private Integer enabled;

        public Integer getEnabled() { return enabled; }
        public void setEnabled(Integer enabled) { this.enabled = enabled; }
    }
}
