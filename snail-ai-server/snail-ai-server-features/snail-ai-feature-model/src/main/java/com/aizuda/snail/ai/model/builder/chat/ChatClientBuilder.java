package com.aizuda.snail.ai.model.builder.chat;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.adapter.server.ServerModelFacade;
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
 * ChatClient 动态构建器。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChatClientBuilder {

    private final ServerModelFacade serverModelFacade;
    private final List<StreamAdvisor> snailChatAdvisors;

    public ChatClient getOrBuildChatClient(String decryptedApiKey, ModelConfigInfoDTO config) {
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
                    e);
        }
    }

    public ChatClient buildChatClient(ModelConfigInfoDTO config, String decryptedApiKey) {
        try {
            ChatModel chatModel = serverModelFacade.buildChatModel(config, decryptedApiKey);
            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultAdvisors(buildAdvisors())
                    .build();
            log.info("Successfully built ChatClient for config: {} (model: {})",
                    config.getId(), config.getModelKey());
            return chatClient;
        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to build ChatClient for config: {}", config.getId(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.CHAT_CLIENT_BUILD_FAILED,
                    "ChatClient 构建失败: " + e.getMessage(),
                    e);
        }
    }

    private Advisor[] buildAdvisors() {
        List<Advisor> advisors = new ArrayList<>();
        advisors.add(new SimpleLoggerAdvisor());
        advisors.addAll(snailChatAdvisors);
        return advisors.toArray(Advisor[]::new);
    }
}
