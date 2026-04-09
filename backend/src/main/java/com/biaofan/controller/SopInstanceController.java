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

@RestController
@RequestMapping("/api/instance")
@RequiredArgsConstructor
public class SopInstanceController {

    private final SopInstanceService instanceService;

    @GetMapping("/my")
    public Result<List<SopInstance>> myInstances(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String status) {
        return Result.ok(instanceService.getMyInstances(userId, status));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getInstance(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        SopInstance inst = instanceService.getInstance(id);
        return Result.ok(Map.of(
                "instance", inst,
                "sop", Map.of()
        ));
    }

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
