package com.aizuda.snail.ai.agent.chat.starter;

import com.aizuda.snail.ai.agent.chat.api.SnailAiChatAuthorize;
import com.aizuda.snail.ai.agent.chat.api.SnailAiChatCredentialValidator;
import com.aizuda.snail.ai.agent.chat.api.SnailAiChatSession;
import com.aizuda.snail.ai.agent.chat.api.SnailAiChatSessionContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@RequiredArgsConstructor
public class SnailAiChatAuthenticationInterceptor implements HandlerInterceptor {

    private final SnailAiChatTokenService tokenService;
    private final List<SnailAiChatCredentialValidator> credentialValidators;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (!requiresAuthentication(handler)) {
            return true;
        }

        SnailAiChatSession session = tokenService.verify(request.getHeader(SnailAiChatTokenService.AUTH_HEADER));
        for (SnailAiChatCredentialValidator validator : credentialValidators) {
            validator.validate(session);
        }
        SnailAiChatSessionContext.set(session);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        SnailAiChatSessionContext.clear();
    }

    private boolean requiresAuthentication(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), SnailAiChatAuthorize.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), SnailAiChatAuthorize.class);
    }
}
