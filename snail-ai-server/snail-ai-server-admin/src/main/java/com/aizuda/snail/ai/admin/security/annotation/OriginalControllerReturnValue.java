package com.aizuda.snail.ai.admin.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * OriginalControllerReturnValue
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OriginalControllerReturnValue {
}
