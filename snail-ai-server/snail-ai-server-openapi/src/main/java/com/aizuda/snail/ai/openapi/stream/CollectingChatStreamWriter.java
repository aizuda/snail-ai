package com.aizuda.snail.ai.openapi.stream;

import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.feature.agent.stream.ChatStreamWriter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class CollectingChatStreamWriter implements ChatStreamWriter {

    private final StringBuilder buffer = new StringBuilder();
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile Throwable error;

    @Override
    public void send(String data) {
        ChatStreamResponse response = parse(data);
        if (response == null) {
            buffer.append(data);
            return;
        }
        if (ChatStreamResponse.TYPE_TEXT.equals(response.getType()) && response.getContent() != null) {
            buffer.append(response.getContent());
        }
    }

    @Override
    public void complete() {
        latch.countDown();
    }

    @Override
    public void completeWithError(Throwable ex) {
        this.error = ex;
        latch.countDown();
    }

    public String awaitAndGetFullText(long timeoutMs) throws InterruptedException, TimeoutException {
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("对话响应超时");
        }
        if (error != null) {
            throw new SnailAiException("对话执行失败: " + error.getMessage(), error);
        }
        return buffer.toString();
    }

    private ChatStreamResponse parse(String data) {
        try {
            return JsonUtil.parseObject(data.trim(), ChatStreamResponse.class);
        } catch (Exception e) {
            log.debug("Ignore non-json chat stream chunk: {}", data);
            return null;
        }
    }
}
