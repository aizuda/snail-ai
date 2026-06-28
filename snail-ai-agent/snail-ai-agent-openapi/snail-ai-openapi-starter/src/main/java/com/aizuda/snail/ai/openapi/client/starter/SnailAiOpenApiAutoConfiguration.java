package com.aizuda.snail.ai.openapi.client.starter;

import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiAgentClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiChatClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiConversationClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiEmbedClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiUserClient;
import com.aizuda.snail.ai.openapi.client.core.SnailAiOpenApi;
import com.aizuda.snail.ai.openapi.client.core.config.SnailAiOpenApiProperties;
import com.aizuda.snail.ai.openapi.client.core.rpc.RequestBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.net.http.HttpClient;

@AutoConfiguration
@EnableConfigurationProperties({SnailAiOpenApiProperties.class, SnailAiAgentProperties.class})
@ConditionalOnProperty(prefix = "snail-ai.openapi", name = "enabled", havingValue = "true", matchIfMissing = false)
public class SnailAiOpenApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "snailAiOpenApiHttpClient")
    public HttpClient snailAiOpenApiHttpClient(SnailAiOpenApiProperties properties) {
        return RequestBuilder.buildHttpClient(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenApiChatClient openApiChatClient(HttpClient snailAiOpenApiHttpClient,
                                               SnailAiOpenApiProperties openApiProperties,
                                               SnailAiAgentProperties aiAgentProperties) {
        return RequestBuilder.createProxy(OpenApiChatClient.class, snailAiOpenApiHttpClient, openApiProperties, aiAgentProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenApiConversationClient openApiConversationClient(HttpClient snailAiOpenApiHttpClient,
                                                               SnailAiOpenApiProperties properties,
                                                               SnailAiAgentProperties aiAgentProperties) {
        return RequestBuilder.createProxy(OpenApiConversationClient.class, snailAiOpenApiHttpClient, properties, aiAgentProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenApiAgentClient openApiAgentClient(HttpClient snailAiOpenApiHttpClient,
                                                 SnailAiOpenApiProperties properties,
                                                 SnailAiAgentProperties aiAgentProperties) {
        return RequestBuilder.createProxy(OpenApiAgentClient.class, snailAiOpenApiHttpClient, properties, aiAgentProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenApiEmbedClient openApiEmbedClient(HttpClient snailAiOpenApiHttpClient,
                                                 SnailAiOpenApiProperties properties,
                                                 SnailAiAgentProperties aiAgentProperties) {
        return RequestBuilder.createProxy(OpenApiEmbedClient.class, snailAiOpenApiHttpClient, properties, aiAgentProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenApiUserClient openApiUserClient(HttpClient snailAiOpenApiHttpClient,
                                               SnailAiOpenApiProperties properties,
                                               SnailAiAgentProperties aiAgentProperties) {
        return RequestBuilder.createProxy(OpenApiUserClient.class, snailAiOpenApiHttpClient, properties, aiAgentProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SnailAiOpenApi snailAiOpenApi(OpenApiChatClient chatClient,
                                         OpenApiConversationClient conversationClient,
                                         OpenApiAgentClient agentClient,
                                         OpenApiUserClient userClient) {
        SnailAiOpenApi.init(chatClient, conversationClient, agentClient, userClient);
        return new SnailAiOpenApi();
    }
}
