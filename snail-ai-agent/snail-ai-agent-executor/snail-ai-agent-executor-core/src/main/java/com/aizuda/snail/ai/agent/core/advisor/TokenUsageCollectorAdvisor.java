package com.aizuda.snail.ai.agent.core.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 从流式响应的最终 chunk 中提取 Token 使用量，写入 {@link ClientStreamExecutionContext}。
 * <p>
 * 需要模型配置启用 {@code streamUsage(true)}，OpenAI 才会在最终 SSE chunk 中返回 usage 数据。
 *
 * @author opensnail
 * @date 2026-04-20
 */
@Slf4j
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
            log.debug("TokenUsageCollector: chatResponse is null");
            return;
        }
        Usage usage = cr.getMetadata().getUsage();
        if (usage == null) {
            log.debug("TokenUsageCollector: usage is null");
            return;
        }
        int total = usage.getTotalTokens() != null ? usage.getTotalTokens() : 0;
        if (total > 0) {
            int prompt = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
            int completion = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
            int cache = extractCacheTokens(usage);
            state.setPromptTokens(prompt);
            state.setCompletionTokens(completion);
            state.setCacheTokens(cache);
            log.info("TokenUsageCollector: promptTokens={}, completionTokens={}, cacheTokens={}", prompt, completion, cache);
        } else {
            log.debug("TokenUsageCollector: totalTokens={}", total);
        }
    }

    /**
     * 从原生 Usage 对象中通过反射提取缓存命中 Token 数。
     * OpenAI 兼容模型的 nativeUsage 为 CompletionUsage，包含 promptTokensDetails.cachedTokens。
     * 非 OpenAI 兼容模型不支持，返回 0。
     */
    private int extractCacheTokens(Usage usage) {
        try {
            Object nativeUsage = usage.getNativeUsage();
            if (nativeUsage == null) {
                log.info("extractCacheTokens: nativeUsage is null");
                return 0;
            }
            log.info("extractCacheTokens: nativeUsage type={}", nativeUsage.getClass().getName());
            Method ptdMethod = findMethod(nativeUsage.getClass(), "promptTokensDetails");
            if (ptdMethod == null) {
                log.info("extractCacheTokens: promptTokensDetails method not found");
                return 0;
            }
            Object ptdOptional = ptdMethod.invoke(nativeUsage);
            log.info("extractCacheTokens: promptTokensDetails result={}", ptdOptional);
            if (!(ptdOptional instanceof Optional<?> opt) || opt.isEmpty()) {
                log.info("extractCacheTokens: promptTokensDetails is empty");
                return 0;
            }
            Object ptd = opt.get();
            log.info("extractCacheTokens: PromptTokensDetails type={}", ptd.getClass().getName());
            Method ctMethod = findMethod(ptd.getClass(), "cachedTokens");
            if (ctMethod == null) {
                log.info("extractCacheTokens: cachedTokens method not found");
                return 0;
            }
            Object ctOptional = ctMethod.invoke(ptd);
            log.info("extractCacheTokens: cachedTokens result={}", ctOptional);
            if (ctOptional instanceof Optional<?> ctOpt && ctOpt.isPresent()) {
                Object val = ctOpt.get();
                log.info("extractCacheTokens: cachedTokens value={}", val);
                if (val instanceof Long l) {
                    return l.intValue();
                }
                if (val instanceof Integer i) {
                    return i;
                }
            } else {
                log.info("extractCacheTokens: cachedTokens is empty");
            }
        } catch (Exception e) {
            log.warn("extractCacheTokens: exception={}", e.getMessage(), e);
        }
        return 0;
    }

    private Method findMethod(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                return c.getMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
