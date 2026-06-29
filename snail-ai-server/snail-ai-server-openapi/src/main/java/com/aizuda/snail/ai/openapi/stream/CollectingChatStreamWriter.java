package com.aizuda.snail.ai.openapi.stream;

import com.aizuda.snail.ai.common.dto.agent.ChatStreamResponse;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.feature.agent.stream.ChatStreamWriter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 收集型对话流写出器，将流式对话分片累积为完整文本，供同步（阻塞等待）场景一次性获取最终结果。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
public class CollectingChatStreamWriter implements ChatStreamWriter {

    /**
     * 文本累积缓冲区
     */
    private final StringBuilder buffer = new StringBuilder();
    /**
     * 完成信号，complete / completeWithError 时释放
     */
    private final CountDownLatch latch = new CountDownLatch(1);
    /**
     * 执行过程中捕获的异常，非空表示对话失败
     */
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

    /**
     * 阻塞等待对话完成并返回累积的完整文本
     *
     * @param timeoutMs 最大等待毫秒数
     * @return 累积的完整文本
     * @throws InterruptedException 等待期间线程被中断
     * @throws TimeoutException     超时仍未完成
     */
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
