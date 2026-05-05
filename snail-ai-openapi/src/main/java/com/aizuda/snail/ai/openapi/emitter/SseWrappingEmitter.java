package com.aizuda.snail.ai.openapi.emitter;

import com.aizuda.snail.ai.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * SSE 包装适配器
 * <p>
 * 将 AgentChatService 的 ResponseBodyEmitter.send(text) 调用
 * 转换为标准 SSE 事件格式 (event + data)，输出给客户端 SDK 解析。
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Slf4j
public class SseWrappingEmitter extends ResponseBodyEmitter {

    private final SseEmitter delegate;
    private final StringBuilder buffer = new StringBuilder();
    private final String conversationId;

    public SseWrappingEmitter(SseEmitter delegate, String conversationId) {
        super(0L);
        this.delegate = delegate;
        this.conversationId = conversationId;
    }

    @Override
    public void send(Object data) throws IOException {
        String text = data.toString();
        buffer.append(text);
        delegate.send(SseEmitter.event().name("text").data(text));
    }

    @Override
    public void send(Object data, MediaType mediaType) throws IOException {
        send(data);
    }

    @Override
    public void complete() {
        try {
            String doneData = JsonUtil.toJsonString(Map.of(
                    "conversationId", conversationId,
                    "fullText", buffer.toString()));
            delegate.send(SseEmitter.event().name("done").data(doneData));
        } catch (IOException e) {
            log.warn("Failed to send done event", e);
        }
        delegate.complete();
    }

    @Override
    public void completeWithError(Throwable ex) {
        try {
            String errorData = JsonUtil.toJsonString(Map.of(
                    "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error"));
            delegate.send(SseEmitter.event().name("error").data(errorData));
        } catch (IOException e) {
            log.warn("Failed to send error event", e);
        }
        delegate.completeWithError(ex);
    }

    public String getBufferedText() {
        return buffer.toString();
    }
}
