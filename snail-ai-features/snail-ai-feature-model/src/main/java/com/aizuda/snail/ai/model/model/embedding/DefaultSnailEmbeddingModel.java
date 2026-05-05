package com.aizuda.snail.ai.model.model.embedding;

import com.aizuda.snail.ai.common.log.SnailAiLog;
import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.builder.EmbeddingClientBuilder;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.model.AbstractModel;
import com.aizuda.snail.ai.common.model.embedding.EmbeddingVector;
import com.aizuda.snail.ai.common.model.embedding.SnailEmbeddingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 默认向量模型实现
 * 提供统一的向量化接口，支持多提供商和多配置
 *
 * author: opensnail
 * date: 2026-03-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultSnailEmbeddingModel extends AbstractModel implements SnailEmbeddingModel {
    private final ModelConfigHandler modelConfigHandler;
    private final EmbeddingClientBuilder embeddingClientBuilder;

    @Override
    public boolean supports(String modelKey) {
        // 默认支持所有模型（由DynamicEmbeddingCaller负责验证）
        return true;
    }

    @Override
    public SnailEmbeddingResponse embed(EmbeddingModelDTO dto) throws ModelCallException {
        log.info("Embedding single text with model config: {}", dto.text());
        return embed(dto.text(), dto.dimensions());
    }

    @Override
    public SnailEmbeddingResponse embedBatch(EmbeddingBatchModelDTO dto) throws ModelCallException {
        log.info("Embedding batch texts with model config: {}", dto.texts());
        return embedBatch(dto.texts(), dto.dimensions());
    }

    @Override
    public org.springframework.ai.embedding.EmbeddingModel toSpringAiEmbeddingModel() {
        try {
            String decryptedApiKey = decryptApiKey(modelConfigInfo);
            return embeddingClientBuilder.getOrBuildEmbeddingModel(decryptedApiKey, modelConfigInfo);
        } catch (ModelCallException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * 基于配置ID的向量化调用
     * 适用于需要将文本转换为向量的场景（如搜索、相似度计算）
     *
     * @param text          待向量化的文本
     * @return 向量列表
     * @throws ModelCallException 如果配置不存在、无权限、调用失败等
     */
    public SnailEmbeddingResponse embed( String text, Integer dimensions) throws ModelCallException {

        long startTime = System.currentTimeMillis();

        try {
            // 3. 解密 API Key
            String decryptedApiKey = decryptApiKey(modelConfigInfo);

            // 4. 获取或构建 SnailEmbeddingModel
            org.springframework.ai.embedding.EmbeddingModel embeddingModel = embeddingClientBuilder.getOrBuildEmbeddingModel(
                    decryptedApiKey,
                    modelConfigInfo
            );

            // 5. 执行向量化
            SnailAiLog.LOCAL.info("Embedding text with model: {}, config: {}",
                    modelConfigInfo.getModelName(), modelConfigInfo.getId());

            EmbeddingRequest request = new EmbeddingRequest(
                    Collections.singletonList(text),
                    new EmbeddingOptions() {
                        @Override
                        public String getModel() {
                            return modelConfigInfo.getModelKey();
                        }

                        @Override
                        public @Nullable Integer getDimensions() {
                            return dimensions;
                        }
                    }
            );
            EmbeddingResponse response = embeddingModel.call(request);
            // 6. 转换响应并记录日志
            long duration = System.currentTimeMillis() - startTime;
            SnailEmbeddingResponse snailEmbeddingResponse = convertResponse(response, List.of(text), duration);

            SnailAiLog.LOCAL.info("Embedding completed: {}, duration: {}ms",
                    modelConfigInfo.getId(), duration);

            return snailEmbeddingResponse;

        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            SnailAiLog.LOCAL.error("Embedding failed for config: {}, error: {}",
                    modelConfigInfo.getId(), e.getMessage(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.MODEL_CALL_FAILED,
                    "向量化调用失败: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 基于配置ID的批量向量化调用
     * 适用于需要将多个文本转换为向量的场景
     *
     * @param texts         待向量化的文本列表
     * @return 向量列表（与输入顺序对应）
     * @throws ModelCallException 如果配置不存在、无权限、调用失败等
     */
    public SnailEmbeddingResponse embedBatch(List<String> texts, Integer dimensions) throws ModelCallException {
        if (texts == null || texts.isEmpty()) {
            throw new ModelCallException(ModelCallException.ErrorCode.INVALID_PARAMETER,
                    "文本列表不能为空");
        }

        Long userId = getUserId();
        long startTime = System.currentTimeMillis();

        try {

            // 3. 解密 API Key
            String decryptedApiKey = decryptApiKey(modelConfigInfo);

            // 4. 获取或构建 SnailEmbeddingModel
            org.springframework.ai.embedding.EmbeddingModel embeddingModel = embeddingClientBuilder.getOrBuildEmbeddingModel(
                    decryptedApiKey,
                    modelConfigInfo
            );

            // 5. 执行批量向量化
            SnailAiLog.LOCAL.info("Batch embedding {} texts with model: {}, config: {}, userId: {}",
                    texts.size(), modelConfigInfo.getModelName(), modelConfigInfo.getId(), userId);

            EmbeddingRequest request = new EmbeddingRequest(
                    texts,
                    new EmbeddingOptions() {
                        @Override
                        public String getModel() {
                            return modelConfigInfo.getModelKey();
                        }

                        @Override
                        public @Nullable Integer getDimensions() {
                            return dimensions;
                        }
                    }
            );
            EmbeddingResponse response = embeddingModel.call(request);


            // 6. 转换响应并记录日志
            long duration = System.currentTimeMillis() - startTime;
            SnailEmbeddingResponse snailEmbeddingResponse = convertResponse(response, texts, duration);

            SnailAiLog.LOCAL.info("Batch embedding completed: {}, textCount: {}, duration: {}ms",
                    modelConfigInfo.getId(), texts.size(), duration);

            return snailEmbeddingResponse;

        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            SnailAiLog.LOCAL.error("Batch embedding failed for config: {}, error: {}",
                    modelConfigInfo.getId(), e.getMessage(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.MODEL_CALL_FAILED,
                    "批量向量化调用失败: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getUserId() {
        return 0L;
    }


    /**
     * 解密 API Key
     */
    private String decryptApiKey(ModelConfigInfoDTO config) throws ModelCallException {
        try {
            // 调用服务层获取解密的API Key
            String decryptedKey = modelConfigHandler.decryptApiKey(config.getEncryptedApiKey());
            if (!StringUtils.hasText(decryptedKey)) {
                throw new ModelCallException(ModelCallException.ErrorCode.API_KEY_DECRYPT_FAILED,
                        "API Key解密失败或不存在");
            }
            return decryptedKey;
        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            SnailAiLog.LOCAL.error("Failed to decrypt API Key for config: {}", config.getId(), e);
            throw new ModelCallException(ModelCallException.ErrorCode.API_KEY_DECRYPT_FAILED,
                    "API Key解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 估算Token数（简单粗略估算）
     * 实际应该根据模型的分词器来计算
     * 这里简单按照 1 token ≈ 4 字符的比例估算
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 粗略估算：中文约为1字符1token，英文约为4字符1token
        return (int) Math.ceil(text.length() / 3.5);
    }

    /**
     * 将float数组转换为List<Float>
     */
    private List<Float> convertToFloatList(float[] floatArray) {
        List<Float> result = new ArrayList<>();
        if (floatArray != null) {
            for (float f : floatArray) {
                result.add(f);
            }
        }
        return result;
    }

    private SnailEmbeddingResponse convertResponse(
            EmbeddingResponse embeddingResponse, List<String> texts,
            long cost) {

        SnailEmbeddingResponse response = new SnailEmbeddingResponse();
        response.setCostTimeMs(cost);

        List<EmbeddingVector> vectors = new ArrayList<>();

        for (int i = 0; i < embeddingResponse.getResults().size(); i++) {

            float[] vector =
                    embeddingResponse.getResults().get(i).getOutput();

            EmbeddingVector v = new EmbeddingVector();
            v.setIndex(i);
            v.setInput(texts.get(i));
            v.setVector(vector);

            vectors.add(v);
        }

        response.setVectors(vectors);

        if (!vectors.isEmpty()) {
            response.setDimensions(vectors.get(0).getVector().length);
        }

        return response;
    }
}

