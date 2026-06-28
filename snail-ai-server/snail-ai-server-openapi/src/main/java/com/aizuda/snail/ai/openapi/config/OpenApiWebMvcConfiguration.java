package com.aizuda.snail.ai.openapi.config;

import com.aizuda.snail.ai.openapi.interceptor.OpenApiAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * OpenAPI WebMvc 配置，注册认证拦截器到 /openapi/** 路径
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Configuration
@RequiredArgsConstructor
public class OpenApiWebMvcConfiguration implements WebMvcConfigurer {

    private final OpenApiAuthInterceptor openApiAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(openApiAuthInterceptor)
                .addPathPatterns("/openapi/**");
    }
}
