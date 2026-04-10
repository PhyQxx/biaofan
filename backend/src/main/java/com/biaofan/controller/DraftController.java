package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.SopDraft;
import com.biaofan.service.SopDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 草稿控制器
 * <p>提供SOP执行离线草稿的同步功能，支持在无网络环境下保存执行进度，
 * 联网后自动同步到服务器。</p>
 *
 * @RestController
 * @RequiredArgsConstructor
 */
@RestController
@RequiredArgsConstructor
public class DraftController {

    private final SopDraftService draftService;

    /**
     * 离线草稿同步
     * <p>将客户端的草稿数据同步到服务端，存在则更新，不存在则创建。
     * 适用于离线环境下执行SOP时的进度保存。</p>
     *
     * POST /api/sop/executions/draft
     *
     * @param userId 当前登录用户ID
     * @param body   请求体，包含sopId、draftData（草稿JSON）、clientUpdatedAt（客户端时间戳）
     * @return 同步结果，包含draftId和syncedAt
     */
    /**
     * POST /api/sop/executions/draft
     * 离线草稿同步：存在则更新，不存在则创建
     */
    @PostMapping("/api/sop/executions/draft")
    public Result<Map<String, Object>> syncDraft(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> body) {
        try {
            // C-04: executorId 从 @AuthenticationPrincipal 获取，而非请求体
            Long executorId = userId;
            Long sopId = Long.valueOf(body.get("sopId").toString());
            String draftData = body.get("draftData") != null ? body.get("draftData").toString() : "{}";
            Long clientUpdatedAt = body.get("clientUpdatedAt") != null
                ? Long.valueOf(body.get("clientUpdatedAt").toString())
                : System.currentTimeMillis();

            SopDraft draft = draftService.syncDraft(executorId, sopId, draftData, clientUpdatedAt);

            return Result.ok(Map.of(
                "draftId", draft.getId(),
                "syncedAt", draft.getSyncedAt().toString()
            ));
        } catch (Exception e) {
            return Result.fail(400, "草稿同步失败: " + e.getMessage());
        }
    }
}
