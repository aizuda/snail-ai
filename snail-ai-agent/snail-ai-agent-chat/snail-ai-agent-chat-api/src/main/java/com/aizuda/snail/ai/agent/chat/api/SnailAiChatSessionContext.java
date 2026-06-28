package com.aizuda.snail.ai.agent.chat.api;

import java.util.Optional;

/**
 * SnailAiChatSessionContext
 *
 * @author opensnail
 * @since 2026-06-14
 */
public final class SnailAiChatSessionContext {

    private static final ThreadLocal<SnailAiChatSession> SESSION = new ThreadLocal<>();

    private SnailAiChatSessionContext() {
    }

    public static void set(SnailAiChatSession session) {
        SESSION.set(session);
    }

    public static Optional<SnailAiChatSession> get() {
        return Optional.ofNullable(SESSION.get());
    }

    public static SnailAiChatSession current() {
        return get().orElseThrow(() -> new IllegalStateException("Snail AI chat session not initialized"));
    }

    public static void clear() {
        SESSION.remove();
    }
}
