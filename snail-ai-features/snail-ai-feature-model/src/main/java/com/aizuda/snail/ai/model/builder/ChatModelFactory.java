package com.aizuda.snail.ai.model.builder;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import org.springframework.ai.chat.model.ChatModel;

/**
 * ChatModel 工厂接口 - 支持不同提供商的 ChatModel 创建
 * 每个提供商（OpenAI、Claude、Ollama等）都可以实现这个接口
 */
public interface ChatModelFactory {

    /**
     * 获取该工厂支持的提供商标识
     * 例如: "openai", "claude", "ollama" 等
     */
    String getSupportedProvider();

    /**
     * 根据提供商和配置创建 ChatModel
     *
     * @param providerKey   提供商标识 (如 "openai")
     * @param baseUrl       API基础URL (如 https://api.openai.com/v1)
     * @param apiKey        解密后的API Key
     * @param modelKey      模型标识符 (如 "gpt-4")
     * @param configJson    配置JSON (包含temperature等参数)
     * @return 创建好的 ChatModel
     * @throws Exception 如果创建失败
     */
    ChatModel createChatModel(String providerKey, String baseUrl, String apiKey,
                             String modelKey, ConfigExtAttrsDTO configJson) throws Exception;

    /**
     * 验证提供商配置是否有效
     */
    default boolean isConfigValid(String baseUrl, String apiKey) {
        return baseUrl != null && !baseUrl.isEmpty()
                && apiKey != null && !apiKey.isEmpty();
    }
}
