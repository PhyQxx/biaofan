package com.biaofan.service;

import com.biaofan.entity.Organization;
import com.biaofan.entity.OrganizationMember;

import java.util.List;
import java.util.Map;

public interface OrganizationService {
    /**
     * 创建组织
     */
    Organization createOrganization(Long userId, String name, String description, String logoUrl);

    /**
     * 创建子组织（部门）
     */
    Organization createSubOrganization(Long userId, Long parentId, String name, String description, String type);

    /**
     * 获取组织树
     */
    List<Organization> getOrganizationTree(Long rootOrgId);

    /**
     * 加入组织
     */
    void joinOrganization(Long userId, String inviteCode);

    /**
     * 获取用户加入的组织列表
     */
    List<Organization> getUserOrganizations(Long userId);

    /**
     * 获取组织成员列表
     */
    List<Map<String, Object>> getOrganizationMembers(Long orgId);

    /**
     * 刷新邀请码
     */
    String refreshInviteCode(Long userId, Long orgId);
    
    /**
     * 验证用户是否在组织中
     */
    boolean isMember(Long userId, Long orgId);

    /**
     * 获取组织所有者ID
     */
    Long getOwnerId(Long orgId);

    /**
     * 获取组织管理员(owner + admin)列表
     */
    List<Long> listAdmins(Long orgId);
}
