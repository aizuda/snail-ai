package com.aizuda.snail.ai.model.adapter.server;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.model.chat.ChatModelSpec;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 将服务端模型配置组装为客户端 dispatch 模型配置。
 */
@Component
@RequiredArgsConstructor
public class DispatchModelConfigAssembler {

    private final ModelConfigHandler modelConfigHandler;
    private final ServerModelSpecFactory serverModelSpecFactory;

    public ChatDispatchRequest.ModelConfig assembleChatModelConfig(Long modelConfigId) {
        ModelConfigInfoDTO configInfo = modelConfigHandler.getConfigInfo(modelConfigId);
        String decryptedApiKey = modelConfigHandler.decryptApiKey(configInfo.getEncryptedApiKey());
        return assembleChatModelConfig(configInfo, decryptedApiKey);
    }

    public ChatDispatchRequest.ModelConfig assembleChatModelConfig(ModelConfigInfoDTO configInfo, String decryptedApiKey) {
        ChatModelSpec spec = serverModelSpecFactory.chatSpec(configInfo, decryptedApiKey);
        return ChatDispatchRequest.ModelConfig.builder()
                .modelConfigId(configInfo.getId())
                .providerKey(spec.providerKey())
                .adapterKey(spec.adapterKey())
                .modelKey(spec.modelKey())
                .apiEndpoint(spec.baseUrl())
                .apiKey(spec.apiKey())
                .configJson(spec.configJson())
                .build();
    }
}
