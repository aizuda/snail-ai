package com.aizuda.snail.ai.model.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.log.SnailAiLog;
import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.common.model.RerankApiClient;
import com.aizuda.snail.ai.common.model.embedding.EmbeddingVector;
import com.aizuda.snail.ai.common.model.embedding.SnailEmbeddingResponse;
import com.aizuda.snail.ai.model.builder.chat.ChatClientBuilder;
import com.aizuda.snail.ai.model.builder.embedding.EmbeddingClientBuilder;
import com.aizuda.snail.ai.model.builder.rerank.RerankClientBuilder;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRuntimeHandler {

    private final ModelConfigHandler modelConfigHandler;
    private final ChatClientBuilder chatClientBuilder;
    private final EmbeddingClientBuilder embeddingClientBuilder;
    private final RerankClientBuilder rerankClientBuilder;

    public String chat(ChatRequest request) {
        if (request == null) {
            throw invalid("ChatRequest 不能为空");
        }
        validateChatInput(request.modelConfigId(), request.userContext(), request.systemContext());
        long startTime = System.currentTimeMillis();
        ModelConfigInfoDTO config = requireConfig(request.modelConfigId());
        try {
            ChatClient chatClient = buildChatClient(config);
            String result = chatClient.prompt(buildPrompt(request.userContext(), request.systemContext()))
                    .call()
                    .content();
            SnailAiLog.LOCAL.info("Model call completed: {}, duration: {}ms",
                    config.getId(), System.currentTimeMillis() - startTime);
            return result;
        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            SnailAiLog.LOCAL.error("Model call failed for config: {}, error: {}",
                    config.getId(), e.getMessage(), e);
            throw modelCallFailed("模型调用失败: " + e.getMessage(), e);
        }
    }

    public Disposable chatStream(ChatStreamRequest request) {
        if (request == null) {
            throw invalid("ChatStreamRequest 不能为空");
        }
        validateChatInput(request.modelConfigId(), request.userContext(), request.systemContext());
        long startTime = System.currentTimeMillis();
        ModelConfigInfoDTO config = requireConfig(request.modelConfigId());
        try {
            ChatClient chatClient = buildChatClient(config);
            SnailAiLog.LOCAL.info("Stream calling model: {}, config: {}",
                    config.getModelName(), config.getId());
            return chatClient.prompt(buildPrompt(request.userContext(), request.systemContext()))
                    .stream()
                    .chatResponse()
                    .subscribe(
                            chatResponse -> acceptText(chatResponse, request),
                            error -> {
                                SnailAiLog.LOCAL.error("Stream call failed for model: {}, error: {}",
                                        config.getId(), error.getMessage(), error);
                                if (request.onError() != null) {
                                    request.onError().accept(error);
                                }
                            },
                            () -> {
                                SnailAiLog.LOCAL.info("Stream call completed for model: {}, duration: {}ms",
                                        config.getId(), System.currentTimeMillis() - startTime);
                                if (request.onComplete() != null) {
                                    request.onComplete().run();
                                }
                            });
        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            SnailAiLog.LOCAL.error("Stream call failed for config: {}, error: {}",
                    config.getId(), e.getMessage(), e);
            throw modelCallFailed("模型调用失败: " + e.getMessage(), e);
        }
    }

    public SnailEmbeddingResponse embed(EmbeddingRequestDTO request) {
        if (request == null || StrUtil.isBlank(request.text())) {
            throw invalid("文本不能为空");
        }
        return embedBatch(new EmbeddingBatchRequest(request.modelConfigId(), List.of(request.text()), request.dimensions()));
    }

    public SnailEmbeddingResponse embedBatch(EmbeddingBatchRequest request) {
        if (request == null || CollUtil.isEmpty(request.texts())) {
            throw invalid("文本列表不能为空");
        }
        long startTime = System.currentTimeMillis();
        ModelConfigInfoDTO config = requireConfig(request.modelConfigId());
        try {
            EmbeddingModel embeddingModel = buildEmbeddingModel(config);
            EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(
                    request.texts(), embeddingOptions(config, request.dimensions())));
            SnailEmbeddingResponse embeddingResponse = convertEmbeddingResponse(
                    response, request.texts(), System.currentTimeMillis() - startTime);
            SnailAiLog.LOCAL.info("Embedding completed: {}, textCount: {}, duration: {}ms",
                    config.getId(), request.texts().size(), embeddingResponse.getCostTimeMs());
            return embeddingResponse;
        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            SnailAiLog.LOCAL.error("Embedding failed for config: {}, error: {}",
                    config.getId(), e.getMessage(), e);
            throw modelCallFailed("向量化调用失败: " + e.getMessage(), e);
        }
    }

    public EmbeddingModel buildEmbeddingModel(Long modelConfigId) {
        return buildEmbeddingModel(requireConfig(modelConfigId));
    }

    public List<RerankApiClient.RerankResultItem> rerank(RerankRequest request) {
        if (request == null || CollUtil.isEmpty(request.documents())) {
            return List.of();
        }
        ModelConfigInfoDTO config = requireConfig(request.modelConfigId());
        try {
            RerankApiClient client = rerankClientBuilder.buildRerankClient(decryptApiKey(config), config);
            int outputTopN = request.topN() != null ? request.topN() : request.documents().size();
            return client.rerank(request.query(), request.documents(), outputTopN);
        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            SnailAiLog.LOCAL.error("Rerank failed for config: {}, error: {}",
                    config.getId(), e.getMessage(), e);
            throw modelCallFailed("重排调用失败: " + e.getMessage(), e);
        }
    }

    private ChatClient buildChatClient(ModelConfigInfoDTO config) {
        return chatClientBuilder.getOrBuildChatClient(decryptApiKey(config), config);
    }

    private EmbeddingModel buildEmbeddingModel(ModelConfigInfoDTO config) {
        return embeddingClientBuilder.getOrBuildEmbeddingModel(decryptApiKey(config), config);
    }

    private ModelConfigInfoDTO requireConfig(Long modelConfigId) {
        if (modelConfigId == null || modelConfigId <= 0) {
            throw invalid("modelConfigId 不能为空且必须大于0");
        }
        ModelConfigInfoDTO config = modelConfigHandler.getConfigInfo(modelConfigId);
        if (config == null) {
            throw invalid("模型配置不存在: " + modelConfigId);
        }
        return config;
    }

    private String decryptApiKey(ModelConfigInfoDTO config) {
        try {
            String decryptedKey = modelConfigHandler.decryptApiKey(config.getEncryptedApiKey());
            if (StrUtil.isBlank(decryptedKey)) {
                throw new ModelCallException(
                        ModelCallException.ErrorCode.API_KEY_DECRYPT_FAILED,
                        "API Key解密失败或不存在");
            }
            return decryptedKey;
        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            SnailAiLog.LOCAL.error("Failed to decrypt API Key for config: {}", config.getId(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.API_KEY_DECRYPT_FAILED,
                    "API Key解密失败: " + e.getMessage(), e);
        }
    }

    private void validateChatInput(Long modelConfigId, String userContext, String systemContext) {
        if (modelConfigId == null || modelConfigId <= 0) {
            throw invalid("模型配置ID不能为空且必须大于0");
        }
        if (StrUtil.isBlank(userContext) && StrUtil.isBlank(systemContext)) {
            throw invalid("模型输入内容不能为空");
        }
    }

    private Prompt buildPrompt(String userContext, String systemContext) {
        List<Message> messages = new ArrayList<>();
        if (StrUtil.isNotBlank(systemContext)) {
            messages.add(new SystemMessage(systemContext));
        }
        if (StrUtil.isNotBlank(userContext)) {
            messages.add(new UserMessage(userContext));
        }
        return new Prompt(messages);
    }

    private EmbeddingOptions embeddingOptions(ModelConfigInfoDTO config, Integer dimensions) {
        return new EmbeddingOptions() {
            @Override
            public String getModel() {
                return config.getModelKey();
            }

            @Override
            public @Nullable Integer getDimensions() {
                return dimensions;
            }
        };
    }

    private void acceptText(ChatResponse chatResponse, ChatStreamRequest request) {
        String text = chatResponse != null
                && chatResponse.getResult() != null
                && chatResponse.getResult().getOutput() != null
                ? chatResponse.getResult().getOutput().getText()
                : null;
        if (StrUtil.isNotBlank(text) && request.messageConsumer() != null) {
            request.messageConsumer().accept(text);
        }
    }

    private SnailEmbeddingResponse convertEmbeddingResponse(EmbeddingResponse embeddingResponse,
                                                            List<String> texts,
                                                            long costTimeMs) {
        List<EmbeddingVector> vectors = new ArrayList<>();
        for (int i = 0; i < embeddingResponse.getResults().size(); i++) {
            EmbeddingVector vector = new EmbeddingVector();
            vector.setIndex(i);
            vector.setInput(texts.get(i));
            vector.setVector(embeddingResponse.getResults().get(i).getOutput());
            vectors.add(vector);
        }
        SnailEmbeddingResponse response = new SnailEmbeddingResponse();
        response.setCostTimeMs(costTimeMs);
        response.setVectors(vectors);
        if (CollUtil.isNotEmpty(vectors)) {
            response.setDimensions(vectors.get(0).getVector().length);
        }
        return response;
    }

    private static ModelCallException invalid(String message) {
        return new ModelCallException(ModelCallException.ErrorCode.INVALID_PARAMETER, message);
    }

    private static ModelCallException modelCallFailed(String message, Throwable cause) {
        return new ModelCallException(ModelCallException.ErrorCode.MODEL_CALL_FAILED, message, cause);
    }

    public record ChatRequest(Long modelConfigId, String userContext, String systemContext) {
    }

    public record ChatStreamRequest(Long modelConfigId,
                                    String userContext,
                                    String systemContext,
                                    java.util.function.Consumer<String> messageConsumer,
                                    Runnable onComplete,
                                    java.util.function.Consumer<Throwable> onError) {
    }

    public record EmbeddingRequestDTO(Long modelConfigId, String text, Integer dimensions) {
    }

    public record EmbeddingBatchRequest(Long modelConfigId, List<String> texts, Integer dimensions) {
    }

    public record RerankRequest(Long modelConfigId, String query, List<String> documents, Integer topN) {
    }
}
