package com.aizuda.snail.ai.openapi.client.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * OpenAPI 接口方法映射注解
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpenApiMapping {
    String path();
    HttpMethod method() default HttpMethod.GET;

    enum HttpMethod {
        GET, POST, PUT, DELETE
    }
}
