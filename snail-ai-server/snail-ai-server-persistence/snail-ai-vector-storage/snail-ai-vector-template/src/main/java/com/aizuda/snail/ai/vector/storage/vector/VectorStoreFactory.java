package com.aizuda.snail.ai.vector.storage.vector;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import com.aizuda.snail.ai.common.enums.rag.StoreInstanceTypeEnum;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.model.service.ModelRuntimeHandler;
import com.aizuda.snail.ai.persistence.admin.mapper.StoreInstanceMapper;
import com.aizuda.snail.ai.persistence.admin.po.StoreInstancePO;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import com.aizuda.snail.ai.vector.storage.embedding.EmbeddingModelDimensionService;
import com.aizuda.snail.ai.vector.storage.enums.VectorStoreType;
import com.aizuda.snail.ai.vector.storage.exception.VectorStoreException;
import com.aizuda.snail.ai.vector.storage.vector.api.SnailAiVectorStore;

/**
 * 向量库工厂：连接与索引参数均来自「存储实例」表 config JSON。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorStoreFactory {

    public static final Map<VectorStoreType, Function<VectorStoreConfigDTO, SnailAiVectorStore>> REGISTER =
            new HashMap<>();
    private static final String CACHE_KEY_SEPARATOR = "_";
    private static final ConcurrentHashMap<String, CachedVectorStore> STORE_INSTANCE_CACHE = new ConcurrentHashMap<>();

    private final ModelRuntimeHandler modelRuntimeHandler;
    private final ModelConfigHandler modelConfigHandler;
    private final StoreInstanceMapper storeInstanceMapper;
    private final EmbeddingModelDimensionService embeddingModelDimensionService;

    public SnailAiVectorStore create(RagPO knowledge) {
        Integer dimension = knowledge.getDimensionOfVectorModel();
        if (dimension == null) {
            dimension = embeddingModelDimensionService.getEmbeddingDimension(knowledge.getEmbeddingModelId());
        }
        return createForStoreInstance(
                knowledge.getVectorStoreInstanceId(),
                knowledge.getEmbeddingModelId(),
                dimension);
    }

    /**
     * 按存储实例 ID + 嵌入模型创建向量库（知识库 / 记忆配置共用）
     */
    public SnailAiVectorStore createForStoreInstance(Long vectorStoreInstanceId,
                                                     Long embeddingModelId,
                                                     Integer dimensionOfVectorModel) {
        if (vectorStoreInstanceId == null) {
            throw new VectorStoreException("vectorStoreInstanceId 不能为空");
        }
        if (embeddingModelId == null) {
            throw new VectorStoreException("embeddingModelId 不能为空");
        }

        StoreInstancePO inst = getStoreInstanceOrThrow(vectorStoreInstanceId);
        StoreInstanceTypeEnum typeEnum = getStoreInstanceTypeOrThrow(inst);
        ModelConfigInfoDTO modelConfig = getEmbeddingModelConfigOrThrow(embeddingModelId);
        // 缓存版本：存储实例和 Embedding 模型任一配置变更（地址、API Key、模型名、维度等）
        // 都会反映在各自的 updateDt 上，从而触发缓存重建
        LocalDateTime storeUpdateDt = inst.getUpdateDt();
        LocalDateTime modelUpdatedDt = modelConfig.getUpdatedDt();
        String cacheKey = buildCacheKey(vectorStoreInstanceId, embeddingModelId);
        CachedVectorStore cachedVectorStore = STORE_INSTANCE_CACHE.compute(cacheKey, (k, cached) -> {
            if (cached != null && cached.matches(storeUpdateDt, modelUpdatedDt)) {
                return cached;
            }
            if (cached != null) {
                log.info("配置已变更，重新构建 VectorStore 缓存: vectorStoreInstanceId={}, "
                                + "embeddingModelId={}",
                        vectorStoreInstanceId, embeddingModelId);
            }
            return new CachedVectorStore(
                    storeUpdateDt,
                    modelUpdatedDt,
                    buildVectorStore(inst, typeEnum, modelConfig, dimensionOfVectorModel));
        });
        return cachedVectorStore.vectorStore();
    }

    private StoreInstancePO getStoreInstanceOrThrow(Long vectorStoreInstanceId) {
        StoreInstancePO inst = storeInstanceMapper.selectById(vectorStoreInstanceId);
        if (inst == null) {
            throw new VectorStoreException("向量库实例不存在: " + vectorStoreInstanceId);
        }
        return inst;
    }

    private StoreInstanceTypeEnum getStoreInstanceTypeOrThrow(StoreInstancePO inst) {
        StoreInstanceTypeEnum typeEnum = StoreInstanceTypeEnum.fromType(inst.getType());
        if (typeEnum == null) {
            throw new VectorStoreException("不支持的向量库类型: " + inst.getType());
        }
        return typeEnum;
    }

    private ModelConfigInfoDTO getEmbeddingModelConfigOrThrow(Long embeddingModelId) {
        ModelConfigInfoDTO modelConfig = modelConfigHandler.getConfigInfo(embeddingModelId);
        if (modelConfig == null) {
            throw new VectorStoreException("Embedding 模型不存在: " + embeddingModelId);
        }
        return modelConfig;
    }

    private SnailAiVectorStore buildVectorStore(StoreInstancePO inst,
                                                StoreInstanceTypeEnum typeEnum,
                                                ModelConfigInfoDTO modelConfig,
                                                Integer dimensionOfVectorModel) {
        VectorStoreType vectorStoreType = VectorStoreType.valueOf(typeEnum.name());
        Function<VectorStoreConfigDTO, SnailAiVectorStore> factory = REGISTER.get(vectorStoreType);
        if (factory == null) {
            throw new VectorStoreException("向量库类型未注册: " + vectorStoreType);
        }
        EmbeddingModel model = modelRuntimeHandler.buildEmbeddingModel(modelConfig);
        return factory.apply(
                VectorStoreConfigDTO
                        .builder()
                        .config(inst.getConfig())
                        .dimensions(dimensionOfVectorModel)
                        .embeddingModel(model)
                        .build()
        );
    }

    private String buildCacheKey(Long vectorStoreInstanceId, Long embeddingModelId) {
        return vectorStoreInstanceId + CACHE_KEY_SEPARATOR + embeddingModelId;
    }

    private record CachedVectorStore(LocalDateTime storeUpdateDt,
                                      LocalDateTime modelUpdatedDt,
                                      SnailAiVectorStore vectorStore) {

        private boolean matches(LocalDateTime currentStoreUpdateDt, LocalDateTime currentModelUpdatedDt) {
            return Objects.equals(storeUpdateDt, currentStoreUpdateDt)
                    && Objects.equals(modelUpdatedDt, currentModelUpdatedDt);
        }
    }

}
