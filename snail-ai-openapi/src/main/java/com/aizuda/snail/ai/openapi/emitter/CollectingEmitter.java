package com.aizuda.snail.ai.openapi.emitter;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 收集式 Emitter，用于同步对话模式
 * <p>
 * 将流式 chat 的所有 chunk 收集到 buffer，
 * 通过 CountDownLatch 阻塞等待完成，返回完整文本。
 *
 * @author opensnail
 * @date 2026-04-24
 */
@Slf4j
public class CollectingEmitter extends ResponseBodyEmitter {

    private final StringBuilder buffer = new StringBuilder();
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile Throwable error;

    public CollectingEmitter() {
        super(0L);
    }

    @Override
    public void send(Object data) throws IOException {
        buffer.append(data);
    }

    @Override
    public void send(Object data, MediaType mediaType) throws IOException {
        buffer.append(data);
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
}
