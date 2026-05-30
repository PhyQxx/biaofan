package com.biaofan.aspect;

import com.biaofan.constant.annotation.CheckOrg;
import com.biaofan.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OrgPermissionAspect {

    private final OrganizationService organizationService;

    @Before("@annotation(checkOrg)")
    public void doBefore(JoinPoint joinPoint, CheckOrg checkOrg) {
        Object userIdObj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(userIdObj instanceof Long userId)) {
            throw new RuntimeException("用户未登录");
        }

        String orgIdParamName = checkOrg.value();
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();

        Long orgId = null;
        for (int i = 0; i < parameterNames.length; i++) {
            if (orgIdParamName.equals(parameterNames[i])) {
                orgId = (Long) args[i];
                break;
            }
        }

        if (orgId != null) {
            if (!organizationService.isMember(userId, orgId)) {
                log.warn("[OrgPermission] Security Breach: User {} tried to access Org {}", userId, orgId);
                throw new RuntimeException("无权访问该组织数据");
            }
        }
    }
}
