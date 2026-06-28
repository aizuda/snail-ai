package com.aizuda.snail.ai.agent.core.advisor;

/**
 * Spring AI Advisor 执行顺序。
 */
public final class AdvisorOrder {

    public static final int INTERCEPTOR_CHAIN = 300;
    public static final int TOKEN_USAGE_COLLECTOR = 350;
    public static final int THINKING_COLLECTOR = 400;
    public static final int STREAM_CHUNK_FORWARDER = 500;

    private AdvisorOrder() {
    }
}
