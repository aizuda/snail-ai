package com.aizuda.snail.ai.feature.agent.stream;

/**
 * Chat stream output port used by the agent chain.
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface ChatStreamWriter {

    void send(String data);

    void complete();

    void completeWithError(Throwable ex);
}
