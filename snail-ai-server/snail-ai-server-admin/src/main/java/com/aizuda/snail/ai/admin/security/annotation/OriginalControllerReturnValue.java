package com.aizuda.snail.ai.admin.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记注解：标注后控制器方法的返回值不被全局响应体包装，保持原样返回。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OriginalControllerReturnValue {
}
