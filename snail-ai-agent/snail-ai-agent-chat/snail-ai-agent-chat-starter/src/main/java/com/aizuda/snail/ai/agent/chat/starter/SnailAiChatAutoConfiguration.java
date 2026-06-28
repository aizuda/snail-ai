package com.aizuda.snail.ai.agent.chat.starter;

import com.aizuda.snail.ai.agent.chat.api.SnailAiChatConstants;
import com.aizuda.snail.ai.agent.chat.api.SnailAiChatCredentialValidator;
import com.aizuda.snail.ai.agent.chat.api.SnailAiChatProperties;
import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import com.aizuda.snail.ai.openapi.client.starter.SnailAiOpenApiAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@AutoConfiguration
@AutoConfigureAfter(SnailAiOpenApiAutoConfiguration.class)
@EnableConfigurationProperties(SnailAiChatProperties.class)
@ConditionalOnProperty(prefix = "snail-ai.openapi", name = "enabled", havingValue = "true", matchIfMissing = false)
@ComponentScan("com.aizuda.snail.ai.agent.chat")
public class SnailAiChatAutoConfiguration {

    private static final String CHAT_RESOURCE_LOCATION = "classpath:/META-INF/chat/";

    @Bean
    @ConditionalOnMissingBean
    public SnailAiChatTokenService snailAiChatTokenService(SnailAiAgentProperties agentProperties) {
        return new SnailAiChatTokenService(agentProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SnailAiChatAuthenticationInterceptor snailAiChatAuthenticationInterceptor(
            SnailAiChatTokenService tokenService,
            List<SnailAiChatCredentialValidator> credentialValidators) {
        return new SnailAiChatAuthenticationInterceptor(tokenService, credentialValidators);
    }

    @Bean
    public WebMvcConfigurer snailAiChatWebMvcConfigurer(
            SnailAiChatAuthenticationInterceptor authenticationInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(authenticationInterceptor)
                        .addPathPatterns(SnailAiChatConstants.GATEWAY_PATH,
                                SnailAiChatConstants.GATEWAY_PATH + "/**");
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/snail-chat/**")
                        .addResourceLocations(CHAT_RESOURCE_LOCATION);
            }

        };
    }
}
