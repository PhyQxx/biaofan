package com.biaofan.constant.annotation;

import java.lang.annotation.*;

/**
 * 组织数据权限校验注解
 * 标记在 Controller 方法上，自动校验当前用户是否属于指定的 orgId
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckOrg {
    /** 参数中 orgId 的名称，默认为 "orgId" */
    String value() default "orgId";
}
