package com.aizuda.snail.ai.agent.core.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import reactor.core.publisher.Flux;

/**
 * 从流式响应的最终 chunk 中提取 Token 使用量，写入 {@link ClientStreamExecutionContext}。
 * <p>
 * 需要模型配置启用 {@code streamUsage(true)}，OpenAI 才会在最终 SSE chunk 中返回 usage 数据。
 *
 * @author opensnail
 * @date 2026-04-20
 */
public class TokenUsageCollectorAdvisor implements StreamAdvisor {

    @Override
    public String getName() {
        return "TokenUsageCollectorAdvisor";
    }

    @Override
    public int getOrder() {
        return AdvisorOrder.TOKEN_USAGE_COLLECTOR;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        Object st = request.context().get(ClientAdvisorKeys.STREAM_STATE);
        if (!(st instanceof ClientStreamExecutionContext state)) {
            return chain.nextStream(request);
        }

        return chain.nextStream(request)
                .doOnNext(response -> extractUsage(response, state));
    }

    private void extractUsage(ChatClientResponse response, ClientStreamExecutionContext state) {
        ChatResponse cr = response.chatResponse();
        if (cr == null) {
            return;
        }
        Usage usage = cr.getMetadata().getUsage();
        if (usage.getTotalTokens() > 0) {
            state.setPromptTokens(usage.getPromptTokens());
            state.setCompletionTokens(usage.getCompletionTokens());
        }
    }
}
