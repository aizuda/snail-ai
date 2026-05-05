package com.aizuda.snail.ai.openapi.client.starter;

import com.aizuda.snail.ai.agent.common.config.SnailAiAgentProperties;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiAgentClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiChatClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiConversationClient;
import com.aizuda.snail.ai.openapi.client.core.api.OpenApiUserClient;
import com.aizuda.snail.ai.openapi.client.core.SnailAiOpenApi;
import com.aizuda.snail.ai.openapi.client.core.config.SnailAiOpenApiProperties;
import com.aizuda.snail.ai.openapi.client.core.rpc.RequestBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class SnailAiOpenApiAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "snail-ai.openapi")
    public SnailAiOpenApiProperties snailAiOpenApiProperties() {
        return new SnailAiOpenApiProperties();
    }

    @Bean
    public HttpClient snailAiOpenApiHttpClient(SnailAiOpenApiProperties properties) {
        return RequestBuilder.buildHttpClient(properties);
    }

    @Bean
    public OpenApiChatClient openApiChatClient(HttpClient snailAiOpenApiHttpClient,
                                               SnailAiOpenApiProperties openApiProperties,
                                               SnailAiAgentProperties aiAgentProperties) {
        return RequestBuilder.createProxy(OpenApiChatClient.class, snailAiOpenApiHttpClient, openApiProperties, aiAgentProperties);
    }

    @Bean
    public OpenApiConversationClient openApiConversationClient(HttpClient snailAiOpenApiHttpClient,
                                                               SnailAiOpenApiProperties properties,
                                                               SnailAiAgentProperties aiAgentProperties) {
        return RequestBuilder.createProxy(OpenApiConversationClient.class, snailAiOpenApiHttpClient, properties, aiAgentProperties);
    }

    @Bean
    public OpenApiAgentClient openApiAgentClient(HttpClient snailAiOpenApiHttpClient,
                                                 SnailAiOpenApiProperties properties,
                                                 SnailAiAgentProperties aiAgentProperties) {
        return RequestBuilder.createProxy(OpenApiAgentClient.class, snailAiOpenApiHttpClient, properties, aiAgentProperties);
    }

    @Bean
    public OpenApiUserClient openApiUserClient(HttpClient snailAiOpenApiHttpClient,
                                               SnailAiOpenApiProperties properties,
                                               SnailAiAgentProperties aiAgentProperties) {
        return RequestBuilder.createProxy(OpenApiUserClient.class, snailAiOpenApiHttpClient, properties, aiAgentProperties);
    }

    @Bean
    public SnailAiOpenApi snailAiOpenApi(OpenApiChatClient chatClient,
                                         OpenApiConversationClient conversationClient,
                                         OpenApiAgentClient agentClient,
                                         OpenApiUserClient userClient) {
        SnailAiOpenApi.init(chatClient, conversationClient, agentClient, userClient);
        return new SnailAiOpenApi();
    }
}
