package com.aizuda.snail.ai.agent.common.context;

import io.micrometer.context.ThreadLocalAccessor;

/**
 * 将 AgentChatContextHolder（ThreadLocal）注册到 Reactor 的 context-propagation 机制。
 *
 * 配合 Hooks.enableAutomaticContextPropagation() 使用后，
 * Reactor 会在订阅时自动捕获当前线程的 ChatContext，
 * 并在每个操作符切换线程时恢复，从而保证 Advisor、ObservationHandler
 * 等回调在任意线程上都能通过 AgentChatContextHolder.getContext() 拿到值。
 */
public class AgentChatContextThreadLocalAccessor
        implements ThreadLocalAccessor<AgentChatContextHolder.ChatContext> {

    public static final String KEY = "AgentChatContext";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public AgentChatContextHolder.ChatContext getValue() {
        return AgentChatContextHolder.getContext();
    }

    @Override
    public void setValue(AgentChatContextHolder.ChatContext value) {
        AgentChatContextHolder.setContext(value);
    }

    @Override
    public void setValue() {
        AgentChatContextHolder.clear();
    }
}
