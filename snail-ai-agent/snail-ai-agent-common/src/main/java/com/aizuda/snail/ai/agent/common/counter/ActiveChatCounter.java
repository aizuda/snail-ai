package com.aizuda.snail.ai.agent.common.counter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 活跃对话计数器（Agent 实例级别共享）
 *
 * @author opensnail
 * @date 2025-04-08
 */
public class ActiveChatCounter {

    private final AtomicInteger count = new AtomicInteger(0);

    public int increment() {
        return count.incrementAndGet();
    }

    public int decrement() {
        return count.decrementAndGet();
    }

    public int get() {
        return count.get();
    }
}
