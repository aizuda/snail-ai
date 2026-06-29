package com.aizuda.snail.ai.feature.agent.chain;

/**
 * 智能体对话责任链 Handler 接口
 * <p>每个 Handler 完成自己的职责后调用 {@code chain.proceed(ctx)} 将控制权传递给下一个；
 * 若需短路（如校验失败、错误提前返回），则直接返回不调用 proceed。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface AgentChatHandler {

    /**
     * 处理对话上下文：完成自身职责后由责任链驱动下一个 Handler；
     * 若需短路（校验失败 / 提前结束），将 {@link AgentChatContext#setTerminated(boolean)} 置真并直接返回
     *
     * @param ctx 智能体对话责任链上下文
     */
    void handle(AgentChatContext ctx);
}
