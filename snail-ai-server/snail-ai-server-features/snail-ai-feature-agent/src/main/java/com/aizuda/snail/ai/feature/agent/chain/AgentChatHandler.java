package com.aizuda.snail.ai.feature.agent.chain;

/**
 * 智能体对话责任链 Handler 接口
 * <p>每个 Handler 完成自己的职责后调用 {@code chain.proceed(ctx)} 将控制权传递给下一个；
 * 若需短路（如校验失败、错误提前返回），则直接返回不调用 proceed。
 */
public interface AgentChatHandler {

    void handle(AgentChatContext ctx);
}
