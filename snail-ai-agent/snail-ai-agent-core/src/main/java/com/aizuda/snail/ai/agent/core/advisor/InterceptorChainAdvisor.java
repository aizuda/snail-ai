package com.aizuda.snail.ai.agent.core.advisor;

import com.aizuda.snail.ai.agent.core.interceptor.SnailAiInterceptor;
import com.aizuda.snail.ai.agent.core.interceptor.SnailAiInterceptorChain;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

import java.util.List;

/**
 * 执行 {@link SnailAiInterceptor} 链的 Advisor 包装。
 * <p>
 * 委托给 {@link SnailAiInterceptorChain} 执行 before（正序）/ after（逆序）。
 *
 * @author opensnail
 */
public class InterceptorChainAdvisor implements BaseAdvisor {

    private final SnailAiInterceptorChain chain;

    public InterceptorChainAdvisor(List<SnailAiInterceptor> interceptors) {
        this.chain = new SnailAiInterceptorChain(interceptors);
    }

    @Override
    public String getName() {
        return "InterceptorChainAdvisor";
    }

    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        return chain.applyBefore(request);
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return chain.applyAfter(response);
    }
}
