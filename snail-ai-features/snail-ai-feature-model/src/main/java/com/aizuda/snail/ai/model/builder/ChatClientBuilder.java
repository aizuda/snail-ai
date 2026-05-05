package com.aizuda.snail.ai.model.builder;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatClient 动态构建器
 * 根据模型配置在运行时动态构建 ChatClient 实例
 * 支持多种提供商（OpenAI、Claude 等）
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChatClientBuilder {

    private final List<ChatModelFactory> chatModelFactories;
    private final List<StreamAdvisor> snailChatAdvisors;

    /**
     * 获取或构建 ChatClient
     */
    public ChatClient getOrBuildChatClient(String decryptedApiKey, ModelConfigInfoDTO config)
            throws ModelCallException {
        try {
            log.debug("Building ChatClient for config: {}, provider: {}", config.getId(), config.getProviderKey());
            return buildChatClient(config, decryptedApiKey);
        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to build ChatClient for config: {}", config.getId(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.CHAT_CLIENT_BUILD_FAILED,
                    "ChatClient 构建失败: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 根据模型配置动态构建 ChatClient
     */
    public ChatClient buildChatClient(ModelConfigInfoDTO config, String decryptedApiKey)
            throws ModelCallException {
        try {
            ChatModelFactory factory = getChatModelFactory(config.getProviderKey());
            ChatModel chatModel = factory.createChatModel(
                    config.getProviderKey(),
                    config.getApiEndpoint(),
                    decryptedApiKey,
                    config.getModelKey(),
                    config.getConfigJson()
            );

            List<Advisor> advisors = new ArrayList<>();
            advisors.add(new SimpleLoggerAdvisor());
            advisors.addAll(snailChatAdvisors);
            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultAdvisors(advisors.toArray(Advisor[]::new))
                    .build();

            log.info("Successfully built ChatClient for config: {} (model: {})", config.getId(), config.getModelKey());
            return chatClient;

        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to build ChatClient for config: {}", config.getId(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.CHAT_CLIENT_BUILD_FAILED,
                    "ChatClient 构建失败: " + e.getMessage(),
                    e
            );
        }
    }

    private ChatModelFactory getChatModelFactory(String providerKey) throws ModelCallException {
        ChatModelFactory factory = chatModelFactories.stream().findFirst().orElse(null);
        if (factory == null) {
            throw new ModelCallException(
                    ModelCallException.ErrorCode.PROVIDER_NOT_SUPPORTED,
                    "不支持的提供商: " + providerKey
            );
        }
        return factory;
    }
}
