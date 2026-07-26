package com.aizuda.snail.ai.feature.agent.stream;

/**
 * Chat stream output port used by the agent chain.
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface ChatStreamWriter {

    /**
     * 发送一段流式数据块到下游（如前端 SSE / HTTP Emitter）
     *
     * @param data 待发送的数据块
     */
    void send(String data);

    /**
     * 正常结束流并关闭下游输出
     */
    void complete();

    /**
     * 以异常结束流，向下游传递错误并关闭输出
     *
     * @param ex 导致流结束的异常
     */
    void completeWithError(Throwable ex);
}
