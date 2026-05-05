package com.aizuda.snail.ai.model.model;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.model.chat.ChatModel;
import com.aizuda.snail.ai.model.model.embedding.SnailEmbeddingModel;
import com.aizuda.snail.ai.model.model.rerank.RerankModel;
import com.aizuda.snail.ai.model.enums.ModelTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * author: opensnail
 * date: 2026-03-04
 */
@Component
@RequiredArgsConstructor
public class ModelFactory {
    private final List<ChatModel> chatModels;
    private final List<SnailEmbeddingModel> snailEmbeddingModels;
    private final List<RerankModel> rerankModels;
    private final ModelConfigHandler modelConfigHandler;

    public Model getModel(Long modelConfigId) {
        ModelConfigInfoDTO configInfo = modelConfigHandler.getConfigInfo(modelConfigId);
        String modelType = configInfo.getModelType();

        ModelTypeEnum typeEnum = ModelTypeEnum.fromValue(modelType);
        if (typeEnum == null) {
            throw new ModelCallException(
                    ModelCallException.ErrorCode.MODEL_CALL_FAILED,
                    "未知的模型类型: " + modelType);
        }

        Model model = resolveImplementation(typeEnum, configInfo);
        if (model == null) {
            throw new ModelCallException(
                    ModelCallException.ErrorCode.MODEL_CALL_FAILED,
                    "未找到模型实现或类型不支持: type=" + modelType + ", modelName=" + configInfo.getModelName());
        }
        model.setModelConfigInfo(configInfo);
        return model;
    }

    private Model resolveImplementation(ModelTypeEnum typeEnum, ModelConfigInfoDTO configInfo) {
        if (typeEnum == ModelTypeEnum.CHAT) {
            return chatModels.stream()
                    .filter(chatModel -> chatModel.supports(configInfo.getModelKey()))
                    .findFirst()
                    .orElse(null);
        }
        if (typeEnum == ModelTypeEnum.EMBEDDING) {
            return snailEmbeddingModels.stream()
                    .filter(embeddingModel -> embeddingModel.supports(configInfo.getModelKey()))
                    .findFirst()
                    .orElse(null);
        }
        if (typeEnum == ModelTypeEnum.RERANKER) {
            return rerankModels.stream()
                    .filter(m -> m.supports(configInfo.getModelKey()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

}
