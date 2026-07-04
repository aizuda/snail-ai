package com.aizuda.snail.ai.admin.service.model;

import com.aizuda.snail.ai.admin.vo.model.ModelTestResultVO;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.model.builder.chat.ChatClientBuilder;
import com.aizuda.snail.ai.model.builder.embedding.EmbeddingClientBuilder;
import com.aizuda.snail.ai.model.builder.rerank.RerankClientBuilder;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.persistence.model.mapper.AiModelConfigMapper;
import com.aizuda.snail.ai.persistence.model.po.AiModelConfigPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型连通性验证服务，支持 Chat / Embedding / Reranker 三种模型类型的最小化测试调用。
 *
 * @author opensnail
 * @since 2026-07-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelTestService {

    private final AiModelConfigMapper configMapper;
    private final ModelConfigHandler modelConfigHandler;
    private final ChatClientBuilder chatClientBuilder;
    private final EmbeddingClientBuilder embeddingClientBuilder;
    private final RerankClientBuilder rerankClientBuilder;

    /**
     * 测试指定模型配置的连通性
     *
     * @param modelId 模型配置 ID
     * @return 验证结果（成功/失败 + 消息 + 耗时）
     */
    public ModelTestResultVO testConnection(Long modelId) {
        AiModelConfigPO po = configMapper.selectById(modelId);
        if (po == null) {
            throw new SnailAiException("模型配置不存在");
        }

        ModelConfigInfoDTO config = modelConfigHandler.getConfigInfo(modelId);
        String decryptedApiKey = modelConfigHandler.decryptApiKey(config.getEncryptedApiKey());

        log.info("开始验证模型连通性: id={}, modelName={}, modelType={}", modelId, config.getModelName(), config.getModelType());

        long startTime = System.currentTimeMillis();
        try {
            String modelType = config.getModelType();
            if (ModelAdapterDefaults.CHAT_MODEL_TYPE.equalsIgnoreCase(modelType)) {
                testChat(config, decryptedApiKey);
            } else if (ModelAdapterDefaults.EMBEDDING_MODEL_TYPE.equalsIgnoreCase(modelType)) {
                testEmbedding(config, decryptedApiKey);
            } else if (ModelAdapterDefaults.RERANKER_MODEL_TYPE.equalsIgnoreCase(modelType)) {
                testRerank(config, decryptedApiKey);
            } else {
                throw new SnailAiException("不支持的模型类型: " + modelType);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("模型连通性验证成功: id={}, modelName={}, elapsed={}ms", modelId, config.getModelName(), elapsed);
            return ModelTestResultVO.builder()
                    .success(true)
                    .message("连接成功")
                    .responseTimeMs(elapsed)
                    .build();
        } catch (SnailAiException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.warn("模型连通性验证失败: id={}, modelName={}, error={}", modelId, config.getModelName(), e.getMessage());
            return ModelTestResultVO.builder()
                    .success(false)
                    .message(e.getMessage())
                    .responseTimeMs(elapsed)
                    .build();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.warn("模型连通性验证失败: id={}, modelName={}, error={}", modelId, config.getModelName(), e.getMessage(), e);
            return ModelTestResultVO.builder()
                    .success(false)
                    .message(extractRootCause(e))
                    .responseTimeMs(elapsed)
                    .build();
        }
    }

    private void testChat(ModelConfigInfoDTO config, String decryptedApiKey) {
        var chatClient = chatClientBuilder.buildChatClient(config, decryptedApiKey);
        chatClient.prompt().user("Hi").call().chatResponse();
    }

    private void testEmbedding(ModelConfigInfoDTO config, String decryptedApiKey) {
        var embeddingModel = embeddingClientBuilder.buildEmbeddingModel(config, decryptedApiKey);
        embeddingModel.embed("test");
    }

    private void testRerank(ModelConfigInfoDTO config, String decryptedApiKey) {
        var rerankClient = rerankClientBuilder.buildRerankClient(decryptedApiKey, config);
        rerankClient.rerank("test", List.of("test"), 1);
    }

    private static String extractRootCause(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = e.getMessage();
        }
        return msg != null ? msg : "未知错误";
    }
}
