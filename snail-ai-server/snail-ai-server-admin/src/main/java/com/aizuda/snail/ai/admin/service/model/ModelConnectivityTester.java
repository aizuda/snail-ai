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
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型连通性测试器（由 ModelTestService 更名而来），支持 Chat / Embedding / Reranker
 * 三种模型类型的最小化一键测试调用：各类型只发一次最小载荷请求，验证 endpoint、API Key、模型名是否可用。
 *
 * <p>测试结果以 {@link ModelTestResultVO}（成功标记 + 消息 + 耗时）返回而非抛异常，
 * 因为"连不通"对管理端是常态化的诊断结果，不是系统错误。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-07-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelConnectivityTester {

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

    /** 对话模型探活：发送最短单轮消息，能拿到响应即视为连通 */
    private void testChat(ModelConfigInfoDTO config, String decryptedApiKey) {
        var chatClient = chatClientBuilder.buildChatClient(config, decryptedApiKey);
        chatClient.prompt().user("Hi").call().chatResponse();
    }

    /** 向量模型探活：对最短文本做一次 embedding，成本最低 */
    private void testEmbedding(ModelConfigInfoDTO config, String decryptedApiKey) {
        var embeddingModel = embeddingClientBuilder.buildEmbeddingModel(config, decryptedApiKey);
        embeddingModel.embed("test");
    }

    /** 重排模型探活：单 query 单文档取 top1，构造最小重排请求 */
    private void testRerank(ModelConfigInfoDTO config, String decryptedApiKey) {
        var rerankClient = rerankClientBuilder.buildRerankClient(decryptedApiKey, config);
        rerankClient.rerank("test", List.of("test"), 1);
    }

    /**
     * 提取最底层异常消息：HTTP 客户端往往把真实原因（如 401、DNS 解析失败）包在多层包装异常里，
     * 直接展示外层消息对管理员没有诊断价值。
     */
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
