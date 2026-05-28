package com.biaofan.controller;

import com.biaofan.dto.Result;
import com.biaofan.entity.Organization;
import com.biaofan.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping("/my")
    public Result<List<Organization>> getMyOrganizations(@AuthenticationPrincipal Long userId) {
        return Result.ok(organizationService.getUserOrganizations(userId));
    }

    @PostMapping
    public Result<Organization> createOrganization(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String desc = payload.get("description");
        String logo = payload.get("logoUrl");
        if (name == null || name.isBlank()) {
            return Result.fail(400, "组织名称不能为空");
        }
        return Result.ok(organizationService.createOrganization(userId, name, desc, logo));
    }

    @PostMapping("/join")
    public Result<Void> joinOrganization(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> payload) {
        String inviteCode = payload.get("inviteCode");
        if (inviteCode == null || inviteCode.isBlank()) {
            return Result.fail(400, "邀请码不能为空");
        }
        try {
            organizationService.joinOrganization(userId, inviteCode);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @GetMapping("/{orgId}/members")
    public Result<List<Map<String, Object>>> getMembers(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orgId) {
        if (!organizationService.isMember(userId, orgId)) {
            return Result.fail(403, "无权查看该组织成员");
        }
        return Result.ok(organizationService.getOrganizationMembers(orgId));
    }

    @PostMapping("/{orgId}/invite-code/refresh")
    public Result<Map<String, String>> refreshInviteCode(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orgId) {
        try {
            String newCode = organizationService.refreshInviteCode(userId, orgId);
            return Result.ok(Map.of("inviteCode", newCode));
        } catch (Exception e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
