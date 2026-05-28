package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.Organization;
import com.biaofan.entity.OrganizationMember;
import com.biaofan.entity.User;
import com.biaofan.mapper.OrganizationMapper;
import com.biaofan.mapper.OrganizationMemberMapper;
import com.biaofan.mapper.UserMapper;
import com.biaofan.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public Organization createOrganization(Long userId, String name, String description, String logoUrl) {
        Organization org = new Organization();
        org.setName(name);
        org.setOwnerId(userId);
        org.setDescription(description);
        org.setLogoUrl(logoUrl);
        org.setInviteCode(generateInviteCode());
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        organizationMapper.insert(org);

        OrganizationMember member = new OrganizationMember();
        member.setOrgId(org.getId());
        member.setUserId(userId);
        member.setRole("owner");
        member.setJoinedAt(LocalDateTime.now());
        organizationMemberMapper.insert(member);

        return org;
    }

    @Override
    @Transactional
    public void joinOrganization(Long userId, String inviteCode) {
        Organization org = organizationMapper.selectOne(
                new LambdaQueryWrapper<Organization>().eq(Organization::getInviteCode, inviteCode)
        );
        if (org == null) {
            throw new RuntimeException("邀请码无效或组织不存在");
        }

        boolean exists = organizationMemberMapper.exists(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getOrgId, org.getId())
                        .eq(OrganizationMember::getUserId, userId)
        );
        if (exists) {
            throw new RuntimeException("您已经加入该组织");
        }

        OrganizationMember member = new OrganizationMember();
        member.setOrgId(org.getId());
        member.setUserId(userId);
        member.setRole("member");
        member.setJoinedAt(LocalDateTime.now());
        organizationMemberMapper.insert(member);
    }

    @Override
    public List<Organization> getUserOrganizations(Long userId) {
        List<OrganizationMember> members = organizationMemberMapper.selectList(
                new LambdaQueryWrapper<OrganizationMember>().eq(OrganizationMember::getUserId, userId)
        );
        if (members.isEmpty()) return List.of();

        List<Long> orgIds = members.stream().map(OrganizationMember::getOrgId).collect(Collectors.toList());
        return organizationMapper.selectBatchIds(orgIds);
    }

    @Override
    public List<Map<String, Object>> getOrganizationMembers(Long orgId) {
        List<OrganizationMember> members = organizationMemberMapper.selectList(
                new LambdaQueryWrapper<OrganizationMember>().eq(OrganizationMember::getOrgId, orgId)
        );
        if (members.isEmpty()) return List.of();

        List<Long> userIds = members.stream().map(OrganizationMember::getUserId).collect(Collectors.toList());
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        return members.stream().map(m -> {
            User u = userMap.get(m.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", m.getUserId());
            item.put("username", u != null ? u.getUsername() : "未知");
            item.put("role", m.getRole());
            item.put("joinedAt", m.getJoinedAt());
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String refreshInviteCode(Long userId, Long orgId) {
        Organization org = organizationMapper.selectById(orgId);
        if (org == null) throw new RuntimeException("组织不存在");
        if (!org.getOwnerId().equals(userId)) {
            throw new RuntimeException("仅所有者可刷新邀请码");
        }

        String newCode = generateInviteCode();
        org.setInviteCode(newCode);
        organizationMapper.updateById(org);
        return newCode;
    }

    @Override
    public boolean isMember(Long userId, Long orgId) {
        if (orgId == null) return true; // NULL is personal space, user always has access to personal space
        return organizationMemberMapper.exists(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getOrgId, orgId)
                        .eq(OrganizationMember::getUserId, userId)
        );
    }

    @Override
    public Long getOwnerId(Long orgId) {
        Organization org = organizationMapper.selectById(orgId);
        return org != null ? org.getOwnerId() : null;
    }

    @Override
    public List<Long> listAdmins(Long orgId) {
        return organizationMemberMapper.selectList(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getOrgId, orgId)
                .in(OrganizationMember::getRole, "owner", "admin"))
                .stream().map(OrganizationMember::getUserId).collect(Collectors.toList());
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
