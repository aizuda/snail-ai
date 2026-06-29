package com.aizuda.snail.ai.model.adapter.server;

import com.aizuda.snail.ai.common.model.RerankApiClient;
import com.aizuda.snail.ai.model.chat.ChatModelRuntime;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.embedding.EmbeddingModelRuntime;
import com.aizuda.snail.ai.model.rerank.RerankModelRuntime;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * 服务端模型构建门面。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Component
@RequiredArgsConstructor
public class ServerModelFacade {

    private final ServerModelSpecFactory specFactory;
    private final ChatModelRuntime chatModelRuntime;
    private final EmbeddingModelRuntime embeddingModelRuntime;
    private final RerankModelRuntime rerankModelRuntime;

    /**
     * 构建聊天模型
     *
     * @param config          模型配置信息
     * @param decryptedApiKey 解密后的 API Key
     * @return 聊天模型实例
     */
    public ChatModel buildChatModel(ModelConfigInfoDTO config, String decryptedApiKey) {
        return chatModelRuntime.build(specFactory.chatSpec(config, decryptedApiKey));
    }

    /**
     * 构建向量化模型
     *
     * @param config          模型配置信息
     * @param decryptedApiKey 解密后的 API Key
     * @return 向量化模型实例
     */
    public EmbeddingModel buildEmbeddingModel(ModelConfigInfoDTO config, String decryptedApiKey) {
        return embeddingModelRuntime.build(specFactory.embeddingSpec(config, decryptedApiKey));
    }

    /**
     * 构建重排客户端
     *
     * @param config          模型配置信息
     * @param decryptedApiKey 解密后的 API Key
     * @return 重排客户端
     */
    public RerankApiClient buildRerankClient(ModelConfigInfoDTO config, String decryptedApiKey) {
        return rerankModelRuntime.build(specFactory.rerankSpec(config, decryptedApiKey));
    }
}
