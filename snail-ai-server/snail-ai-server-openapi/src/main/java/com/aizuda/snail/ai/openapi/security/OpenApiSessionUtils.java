package com.aizuda.snail.ai.openapi.security;

import lombok.Builder;
import lombok.Data;

/**
 * OpenAPI 会话工具类（ThreadLocal，与 OpenApiAuthInterceptor 配合）
 *
 * @author opensnail
 * @since 2026-04-24
 */
public final class OpenApiSessionUtils {

    private static final ThreadLocal<OpenApiSession> SESSION = new ThreadLocal<>();

    @Data
    @Builder
    public static class OpenApiSession {
        private String appId;
        private Long appDbId;
        private String appToken;
    }

    /**
     * 绑定当前线程的 OpenAPI 会话
     *
     * @param session 当前线程的 OpenAPI 会话
     */
    public static void set(OpenApiSession session) {
        SESSION.set(session);
    }

    /**
     * 获取当前线程的 OpenAPI 会话
     *
     * @return 当前线程的 OpenAPI 会话；未初始化时抛出 {@link IllegalStateException}
     */
    public static OpenApiSession current() {
        OpenApiSession session = SESSION.get();
        if (session == null) {
            throw new IllegalStateException("OpenAPI session not initialized");
        }
        return session;
    }

    /**
     * 清除当前线程绑定的 OpenAPI 会话
     */
    public static void clear() {
        SESSION.remove();
    }

    private OpenApiSessionUtils() {
    }
}
