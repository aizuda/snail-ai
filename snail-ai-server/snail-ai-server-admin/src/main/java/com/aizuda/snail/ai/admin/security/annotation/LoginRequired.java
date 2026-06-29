package com.aizuda.snail.ai.admin.security.annotation;

import com.aizuda.snail.ai.admin.enums.RoleEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 登录校验注解：标注在方法或类上，要求请求已登录，并可指定所需角色。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {

    /**
     * 是否要求登录
     */
    boolean required() default true;

    /**
     * 所需角色，默认管理员
     */
    RoleEnum role() default RoleEnum.ADMIN;

}
