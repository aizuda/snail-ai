package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.model.adapter.server.DispatchModelConfigAssembler;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.enums.ModelTypeEnum;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 模型解析：确定对话使用的模型，并准备远程分发所需的 {@link ChatDispatchRequest.ModelConfig}
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class ModelResolveHandler implements AgentChatHandler {

    private final ModelConfigHandler modelConfigHandler;
    private final DispatchModelConfigAssembler dispatchModelConfigAssembler;

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
                ctx.getStreamWriter().send("Error: 未配置对话模型");
                ctx.getStreamWriter().complete();
                ctx.setTerminated(true);
                return;
            }
            modelId = model.getId();
        }

        ctx.setModelId(modelId);

        try {
            ChatDispatchRequest.ModelConfig dispatchModel = dispatchModelConfigAssembler.assembleChatModelConfig(modelId);
            ctx.setModelConfig(dispatchModel);
            log.debug("Model config prepared for dispatch: modelId={}", modelId);
        } catch (Exception e) {
            log.error("Failed to prepare model config for remote dispatch, modelId={}", modelId, e);
            ctx.getStreamWriter().send("Error: 模型配置加载失败");
            ctx.getStreamWriter().complete();
            ctx.setTerminated(true);
        }
    }


}
