package com.aizuda.snail.ai.agent.core.executor.client;

import com.aizuda.snail.ai.agent.core.executor.model.ChatModelFactory;
import com.aizuda.snail.ai.agent.core.executor.tool.ToolCallingManagerBuildRequest;
import com.aizuda.snail.ai.agent.core.executor.tool.ToolCallingManagerFactory;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * 默认 ChatClient 工厂。
 */
public class DefaultChatClientFactory implements ChatClientFactory {

    private final ChatModelFactory chatModelFactory;
    private final ToolCallingManagerFactory toolCallingManagerFactory;
    private final Advisor[] defaultAdvisors;
    private final List<ChatClientCustomizer> customizers;

    public DefaultChatClientFactory(DefaultChatClientFactoryConfig config) {
        this.chatModelFactory = config.getChatModelFactory();
        this.toolCallingManagerFactory = config.getToolCallingManagerFactory();
        this.defaultAdvisors = config.getDefaultAdvisors() != null
                ? config.getDefaultAdvisors()
                : new Advisor[0];
        this.customizers = config.getCustomizers() != null ? config.getCustomizers() : List.of();
    }

    @Override
    public ChatClient build(ChatClientBuildRequest request) {
        ChatModel chatModel = chatModelFactory.build(request.getModelConfig());
        ToolCallingManager toolCallingManager = toolCallingManagerFactory.build(
                ToolCallingManagerBuildRequest.builder().build());
        ToolCallingAdvisor toolCallingAdvisor = ToolCallingAdvisor.builder()
                .toolCallingManager(toolCallingManager)
                .build();
        List<ToolCallback> tools = request.getTools() != null ? request.getTools() : List.of();

        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultAdvisors(defaultAdvisors)
                .defaultAdvisors(toolCallingAdvisor)
                .defaultTools(tools.toArray())
                .defaultToolContext(new HashMap<>());
        ChatClientBuildContext context = ChatClientBuildContext.builder()
                .request(request)
                .builder(builder)
                .build();
        customizers.stream()
                .sorted(Comparator.comparingInt(ChatClientCustomizer::order))
                .forEach(customizer -> customizer.customize(context));
        return context.getBuilder().build();
    }

    @Data
    @Builder
    public static class DefaultChatClientFactoryConfig {
        private ChatModelFactory chatModelFactory;
        private ToolCallingManagerFactory toolCallingManagerFactory;
        private Advisor[] defaultAdvisors;
        private List<ChatClientCustomizer> customizers;
    }
}
