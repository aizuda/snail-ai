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

/**
 * 开放 API 鉴权拦截器，校验请求头中的应用 ID 与 Token（应用信息本地缓存 5 分钟），
 * 通过后将应用会话写入上下文，请求结束时清理。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Component
public class OpenApiAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER_APP_ID = "Snail-Ai-App-Id";
    public static final String HEADER_TOKEN = "Snail-Ai-Token";

    private static final int APP_STATUS_ACTIVE = 1;

    /**
     * 应用信息本地缓存（appId → 应用），减少每次请求的数据库查询
     */
    private final LoadingCache<String, AppPO> appCache;

    /**
     * 构造拦截器并初始化应用信息本地缓存
     *
     * @param appMapper 应用数据访问 Mapper（缓存未命中时回源查询）
     */
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
                .appToken(app.getToken())
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
