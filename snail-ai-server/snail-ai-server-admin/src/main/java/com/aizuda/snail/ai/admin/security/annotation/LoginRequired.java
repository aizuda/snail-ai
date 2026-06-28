package com.aizuda.snail.ai.admin.security.annotation;

import com.aizuda.snail.ai.admin.enums.RoleEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {

    boolean required() default true;

    RoleEnum role() default RoleEnum.ADMIN;

}
