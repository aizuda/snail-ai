package com.aizuda.snail.ai.openapi.security;

import lombok.Builder;
import lombok.Data;

/**
 * OpenAPI 会话工具类（ThreadLocal，与 OpenApiAuthInterceptor 配合）
 *
 * @author opensnail
 * @date 2026-04-24
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

    public static void set(OpenApiSession session) {
        SESSION.set(session);
    }

    public static OpenApiSession current() {
        OpenApiSession session = SESSION.get();
        if (session == null) {
            throw new IllegalStateException("OpenAPI session not initialized");
        }
        return session;
    }

    public static void clear() {
        SESSION.remove();
    }

    private OpenApiSessionUtils() {
    }
}
