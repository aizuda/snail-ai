package com.aizuda.snail.ai.agent.core.interceptor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;

import java.util.Comparator;
import java.util.List;

/**
 * 拦截器链执行器
 * <p>
 * 管理 {@link SnailAiInterceptor} 列表的有序执行：
 * <ul>
 *   <li>{@code applyBefore}: 按 Order 正序执行 {@code beforeRequest}</li>
 *   <li>{@code applyAfter}: 按 Order 逆序执行 {@code afterResponse}</li>
 * </ul>
 *
 * @author opensnail
 */
public class SnailAiInterceptorChain {

    private final List<SnailAiInterceptor> interceptors;

    public SnailAiInterceptorChain(List<SnailAiInterceptor> interceptors) {
        this.interceptors = interceptors == null ? List.of() : interceptors.stream()
                .sorted(Comparator.comparingInt(SnailAiInterceptor::getOrder))
                .toList();
    }

    /**
     * 正序执行所有拦截器的 beforeRequest
     */
    public ChatClientRequest applyBefore(ChatClientRequest request) {
        ChatClientRequest current = request;
        for (SnailAiInterceptor interceptor : interceptors) {
            current = interceptor.beforeRequest(current);
        }
        return current;
    }

    /**
     * 逆序执行所有拦截器的 afterResponse
     */
    public ChatClientResponse applyAfter(ChatClientResponse response) {
        ChatClientResponse current = response;
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            current = interceptors.get(i).afterResponse(current);
        }
        return current;
    }

    public List<SnailAiInterceptor> getInterceptors() {
        return interceptors;
    }

    public boolean isEmpty() {
        return interceptors.isEmpty();
    }
}
