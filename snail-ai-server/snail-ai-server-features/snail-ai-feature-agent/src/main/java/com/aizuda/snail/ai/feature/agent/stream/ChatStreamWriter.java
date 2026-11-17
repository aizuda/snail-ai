package com.aizuda.snail.ai.feature.agent.stream;

import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;

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
     * 接收结构化完成帧。默认忽略，保持现有文本流写出器兼容。
     *
     * @param completion Agent completion 帧
     */
    default void onCompletion(ChatStreamResponse completion) {
    }

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
