package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.SopDispatch;
import com.biaofan.service.SopDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sop")
@RequiredArgsConstructor
public class SopDispatchController {

    private final SopDispatchService dispatchService;

    /** 批量分发 SOP */
    @PostMapping("/batch-dispatch")
    public Result<List<SopDispatch>> batchDispatch(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, List<Integer>> body) {
        try {
            List<Integer> templateIdsInt = body.get("templateIds");
            List<Integer> assigneeIdsInt = body.get("assigneeIds");
            
            // Convert Integer to Long
            List<Long> templateIds = templateIdsInt.stream().map(Integer::longValue).collect(Collectors.toList());
            List<Long> assigneeIds = assigneeIdsInt.stream().map(Integer::longValue).collect(Collectors.toList());
            
            List<SopDispatch> results = dispatchService.batchDispatch(userId, templateIds, assigneeIds);
            return Result.ok(results);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 分发记录列表 */
    @GetMapping("/dispatches")
    public Result<List<SopDispatch>> dispatches(@AuthenticationPrincipal Long userId) {
        return Result.ok(dispatchService.getMyDispatches(userId));
    }
}
