package com.aizuda.snail.ai.agent.core.executor.client;

import org.springframework.core.Ordered;

/**
 * ChatClient 横向自定义扩展。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface ChatClientCustomizer {

    /**
     * 自定义器名称，默认取实现类全限定名。
     *
     * @return 自定义器名称
     */
    default String name() {
        return getClass().getName();
    }

    /**
     * 执行顺序，值越小优先级越高。
     *
     * @return 排序值
     */
    default int order() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 在 ChatClient 构建过程中对其 Builder 进行自定义扩展。
     *
     * @param context ChatClient 构建上下文（含构建请求与 Builder）
     */
    void customize(ChatClientBuildContext context);
}
