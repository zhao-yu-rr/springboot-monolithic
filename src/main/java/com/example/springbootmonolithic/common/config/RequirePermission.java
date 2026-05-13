package com.example.springbootmonolithic.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限鉴权注解 - 标注在Controller方法上，要求用户拥有指定的permission_code才能访问
 * 支持指定多个权限编码，用户拥有其中任意一个即可通过鉴权（OR逻辑）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 允许访问的权限编码列表（OR逻辑，满足其一即可）
     */
    String[] value();
}