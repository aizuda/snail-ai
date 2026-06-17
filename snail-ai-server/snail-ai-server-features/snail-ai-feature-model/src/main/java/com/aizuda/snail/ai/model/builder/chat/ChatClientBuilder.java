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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatClient 动态构建器（带缓存）。
 * <p>
 * 缓存 key = configId，避免每次切片/对话都重建 OpenAiChatModel 及其底层 HTTP 连接池。
 * 5 分钟 TTL 自动淘汰过期条目。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChatClientBuilder {

    private static final long CACHE_TTL_MS = 300_000L;

    private final ServerModelFacade serverModelFacade;
    private final List<StreamAdvisor> snailChatAdvisors;

    private final ConcurrentHashMap<String, CacheEntry> clientCache = new ConcurrentHashMap<>();

    public ChatClient getOrBuildChatClient(String decryptedApiKey, ModelConfigInfoDTO config) {
        String cacheKey = String.valueOf(config.getId());
        CacheEntry entry = clientCache.get(cacheKey);
        if (entry != null && !entry.isExpired() && entry.matchesApiKey(decryptedApiKey)) {
            log.debug("Reusing cached ChatClient for config: {}", config.getId());
            return entry.client;
        }
        if (entry != null) {
            log.info("ChatClient cache invalidated for config: {} (expired={}, apiKeyChanged={})",
                    config.getId(), entry.isExpired(), !entry.matchesApiKey(decryptedApiKey));
        }
        try {
            log.debug("Building ChatClient for config: {}, provider: {}", config.getId(), config.getProviderKey());
            ChatClient client = buildChatClient(config, decryptedApiKey);
            clientCache.put(cacheKey, new CacheEntry(client, decryptedApiKey));
            return client;
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

    @Scheduled(fixedDelay = 300_000)
    public void evictExpiredClients() {
        int before = clientCache.size();
        clientCache.entrySet().removeIf(e -> {
            if (e.getValue().isExpired()) {
                log.debug("Evicting expired ChatClient for key: {}", e.getKey());
                return true;
            }
            return false;
        });
        int after = clientCache.size();
        if (before != after) {
            log.info("ChatClient cache eviction: {} -> {} entries", before, after);
        }
    }

    public void invalidateClient(Long configId) {
        String cacheKey = String.valueOf(configId);
        CacheEntry removed = clientCache.remove(cacheKey);
        if (removed != null) {
            log.info("ChatClient cache invalidated for config: {}", configId);
        }
    }

    private Advisor[] buildAdvisors() {
        List<Advisor> advisors = new ArrayList<>();
        advisors.add(new SimpleLoggerAdvisor());
        advisors.addAll(snailChatAdvisors);
        return advisors.toArray(Advisor[]::new);
    }

    private static class CacheEntry {
        final ChatClient client;
        final long createdAt;
        final int apiKeyHash;

        CacheEntry(ChatClient client, String apiKey) {
            this.client = client;
            this.createdAt = System.currentTimeMillis();
            this.apiKeyHash = apiKey.hashCode();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_TTL_MS;
        }

        boolean matchesApiKey(String apiKey) {
            return apiKey.hashCode() == apiKeyHash;
        }
    }
}
