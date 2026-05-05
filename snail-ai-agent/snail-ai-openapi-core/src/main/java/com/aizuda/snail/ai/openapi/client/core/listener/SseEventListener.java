package com.aizuda.snail.ai.openapi.client.core.listener;

/**
 * SSE 事件监听器接口
 * <p>
 * 用于接收流式对话的实时文本、思考过程和完成/错误事件。
 *
 * @author opensnail
 * @date 2026-04-24
 */
public interface SseEventListener {

    void onText(String text);

    default void onThinking(String thinking) {
    }

    void onComplete(String data);

    void onError(String errorMessage);
}
