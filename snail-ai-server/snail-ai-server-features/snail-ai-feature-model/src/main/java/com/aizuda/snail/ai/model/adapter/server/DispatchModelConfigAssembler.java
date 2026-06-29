package com.aizuda.snail.ai.model.adapter.server;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.model.chat.ChatModelSpec;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 将服务端模型配置组装为客户端 dispatch 模型配置。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Component
@RequiredArgsConstructor
public class DispatchModelConfigAssembler {

    private final ModelConfigHandler modelConfigHandler;
    private final ServerModelSpecFactory serverModelSpecFactory;

    /**
     * 按模型配置 ID 组装客户端分发用的聊天模型配置（内部完成查询与 API Key 解密）
     *
     * @param modelConfigId 模型配置 ID
     * @return 客户端 dispatch 用的聊天模型配置
     */
    public ChatDispatchRequest.ModelConfig assembleChatModelConfig(Long modelConfigId) {
        ModelConfigInfoDTO configInfo = modelConfigHandler.getConfigInfo(modelConfigId);
        String decryptedApiKey = modelConfigHandler.decryptApiKey(configInfo.getEncryptedApiKey());
        return assembleChatModelConfig(configInfo, decryptedApiKey);
    }

    /**
     * 按模型配置信息组装客户端分发用的聊天模型配置
     *
     * @param configInfo      模型配置信息
     * @param decryptedApiKey 解密后的 API Key
     * @return 客户端 dispatch 用的聊天模型配置
     */
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
