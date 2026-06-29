package com.aizuda.snail.ai.agent.chat.api;

import java.util.Optional;

/**
 * 对话会话上下文，基于 ThreadLocal 在当前请求线程内保存与获取已认证的对话会话。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public final class SnailAiChatSessionContext {

    private static final ThreadLocal<SnailAiChatSession> SESSION = new ThreadLocal<>();

    private SnailAiChatSessionContext() {
    }

    /**
     * 将会话写入当前线程上下文
     *
     * @param session 对话会话
     */
    public static void set(SnailAiChatSession session) {
        SESSION.set(session);
    }

    /**
     * 获取当前线程的会话（可能为空）
     *
     * @return 会话 Optional
     */
    public static Optional<SnailAiChatSession> get() {
        return Optional.ofNullable(SESSION.get());
    }

    /**
     * 获取当前线程的会话，未初始化时抛出异常
     *
     * @return 对话会话（保证非空）
     */
    public static SnailAiChatSession current() {
        return get().orElseThrow(() -> new IllegalStateException("Snail AI chat session not initialized"));
    }

    /**
     * 清理当前线程的会话
     */
    public static void clear() {
        SESSION.remove();
    }
}
