package com.aizuda.snail.ai.openapi.client.starter;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 Snail AI OpenAPI 客户端
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SnailAiOpenApiAutoConfiguration.class)
public @interface EnableSnailAiOpenApi {
}
