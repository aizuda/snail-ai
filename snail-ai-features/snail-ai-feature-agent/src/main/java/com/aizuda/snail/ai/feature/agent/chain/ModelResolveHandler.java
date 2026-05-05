package com.aizuda.snail.ai.feature.agent.chain;


import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.enums.ModelTypeEnum;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;

import java.io.IOException;
import java.util.Map;

/**
 * 模型解析：确定对话使用的模型，并准备远程分发所需的 {@link ChatDispatchRequest.ModelConfig}
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class ModelResolveHandler implements AgentChatHandler {

    private final ModelConfigHandler modelConfigHandler;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        Long modelId = ctx.getAgent().getChatModelId();
        if (modelId == null) {
            ModelConfigInfoDTO model = modelConfigHandler.getDefaultModelByType(ModelTypeEnum.CHAT.getValue());
            if (model == null) {
                try {
                    ctx.getEmitter().send("Error: 未配置对话模型", MediaType.TEXT_PLAIN);
                    ctx.getEmitter().complete();
                } catch (IOException e) {
                    log.error("写入错误信息失败", e);
                }
                ctx.setTerminated(true);
                return;
            }
            modelId = model.getId();
        }

        ctx.setModelId(modelId);

        try {
            ModelConfigInfoDTO configInfo = modelConfigHandler.getConfigInfo(modelId);
            ChatDispatchRequest.ModelConfig dispatchModel = ChatDispatchRequest.ModelConfig.builder()
                    .modelKey(configInfo.getModelKey())
                    .apiEndpoint(configInfo.getApiEndpoint())
                    .apiKey(modelConfigHandler.decryptApiKey(configInfo.getEncryptedApiKey()))
                    .configJson(configInfo.getConfigJson())
                    .build();
            ctx.setModelConfig(dispatchModel);
            log.debug("Model config prepared for dispatch: modelName={}", configInfo.getModelName());
        } catch (Exception e) {
            log.error("Failed to prepare model config for remote dispatch, modelId={}", modelId, e);
            try {
                ctx.getEmitter().send("Error: 模型配置加载失败", MediaType.TEXT_PLAIN);
                ctx.getEmitter().complete();
            } catch (IOException ex) {
                log.error("写入错误信息失败", ex);
            }
            ctx.setTerminated(true);
        }
    }


}
