package com.aizuda.snail.ai.openapi.interceptor;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiAuthenticationException;
import com.aizuda.snail.ai.openapi.security.OpenApiSessionUtils;
import com.aizuda.snail.ai.persistence.app.mapper.AppMapper;
import com.aizuda.snail.ai.persistence.app.po.AppPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OpenApiAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER_APP_ID = "Snail-Ai-App-Id";
    public static final String HEADER_TOKEN = "Snail-Ai-Token";

    private static final int APP_STATUS_ACTIVE = 1;

    private final LoadingCache<String, AppPO> appCache;

    public OpenApiAuthInterceptor(AppMapper appMapper) {
        this.appCache = CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public AppPO load(String appId) {
                        AppPO app = appMapper.selectOne(
                                new LambdaQueryWrapper<AppPO>().eq(AppPO::getAppId, appId));
                        if (app == null) {
                            throw new SnailAiAuthenticationException("应用不存在: {}", appId);
                        }
                        return app;
                    }
                });
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String appId = request.getHeader(HEADER_APP_ID);
        String token = request.getHeader(HEADER_TOKEN);

        if (StrUtil.isBlank(appId) || StrUtil.isBlank(token)) {
            throw new SnailAiAuthenticationException("缺少认证信息，请提供 Snail-Ai-App-Id 和 Snail-Ai-Token");
        }

        AppPO app;
        try {
            app = appCache.getUnchecked(appId);
        } catch (Exception e) {
            throw new SnailAiAuthenticationException("应用认证失败: {}", appId);
        }

        if (!Objects.equals(app.getStatus(), APP_STATUS_ACTIVE)) {
            throw new SnailAiAuthenticationException("应用已禁用: {}", appId);
        }

        if (!tokenEquals(token, app.getToken())) {
            throw new SnailAiAuthenticationException("Token 验证失败");
        }

        OpenApiSessionUtils.set(OpenApiSessionUtils.OpenApiSession.builder()
                .appId(appId)
                .appDbId(app.getId())
                .build());

        return true;
    }

    private boolean tokenEquals(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        OpenApiSessionUtils.clear();
    }
}
