package com.aizuda.snail.ai.admin.service;

import com.aizuda.snail.ai.admin.vo.VectorDimensionConstraintVO;
import com.aizuda.snail.ai.common.enums.rag.StoreInstanceTypeEnum;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
import com.aizuda.snail.ai.persistence.admin.mapper.StoreInstanceMapper;
import com.aizuda.snail.ai.persistence.admin.po.StoreInstancePO;
import com.aizuda.snail.ai.vector.storage.embedding.EmbeddingModelDimensionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 统一处理向量维度约束（模型上限 + 向量库上限）。
 */
@Service
@RequiredArgsConstructor
public class VectorDimensionConstraintService {

    private static final int MIN_VECTOR_DIMENSION = 64;
    private static final int DEFAULT_VECTOR_STORE_MAX_DIMENSION = 4096;
    private static final Map<StoreInstanceTypeEnum, Integer> VECTOR_STORE_MAX_DIMENSIONS = Map.of(
            StoreInstanceTypeEnum.PG_VECTOR, 2000,
            StoreInstanceTypeEnum.MILVUS, 4096,
            StoreInstanceTypeEnum.ELASTICSEARCH, 4096
    );

    private final EmbeddingModelDimensionService embeddingModelDimensionService;
    private final StoreInstanceMapper storeInstanceMapper;

    public VectorDimensionConstraintVO resolveConstraint(Long embeddingModelId, Long vectorStoreInstanceId) {
        if (embeddingModelId == null) {
            throw new SnailAiCommonException("embeddingModelId 不能为空");
        }
        if (vectorStoreInstanceId == null) {
            throw new SnailAiCommonException("vectorStoreInstanceId 不能为空");
        }
        int modelMaxDimension = getModelMaxDimension(embeddingModelId);
        StoreInstancePO storeInstance = getStoreInstanceOrThrow(vectorStoreInstanceId);
        int storeMaxDimension = getStoreMaxDimension(storeInstance);
        return VectorDimensionConstraintVO.builder()
                .modelMaxDimension(modelMaxDimension)
                .storeMaxDimension(storeMaxDimension)
                .effectiveMaxDimension(Math.min(modelMaxDimension, storeMaxDimension))
                .build();
    }

    public int getModelMaxDimension(Long embeddingModelId) {
        return embeddingModelDimensionService.getEmbeddingDimension(embeddingModelId);
    }

    public void validateRequestedDimension(Integer dimension,
                                           Long embeddingModelId,
                                           Long vectorStoreInstanceId) {
        if (dimension == null) {
            throw new SnailAiCommonException("向量维度必填");
        }
        if (dimension < MIN_VECTOR_DIMENSION) {
            throw new SnailAiCommonException("向量维度不能小于" + MIN_VECTOR_DIMENSION);
        }
        int modelMaxDimension = getModelMaxDimension(embeddingModelId);
        if (dimension > modelMaxDimension) {
            throw new SnailAiCommonException("向量维度不能超过模型最大支持维度: " + modelMaxDimension);
        }
        StoreInstancePO storeInstance = getStoreInstanceOrThrow(vectorStoreInstanceId);
        int storeMaxDimension = getStoreMaxDimension(storeInstance);
        if (dimension > storeMaxDimension) {
            StoreInstanceTypeEnum storeType = StoreInstanceTypeEnum.fromType(storeInstance.getType());
            String storeDesc = storeType != null ? storeType.getDescription() : "向量库";
            throw new SnailAiCommonException("向量维度不能超过" + storeDesc + "支持的最大维度: " + storeMaxDimension);
        }
    }

    private StoreInstancePO getStoreInstanceOrThrow(Long vectorStoreInstanceId) {
        StoreInstancePO storeInstance = storeInstanceMapper.selectById(vectorStoreInstanceId);
        if (storeInstance == null) {
            throw new SnailAiCommonException("存储实例不存在");
        }
        return storeInstance;
    }

    private int getStoreMaxDimension(StoreInstancePO storeInstance) {
        StoreInstanceTypeEnum storeType = StoreInstanceTypeEnum.fromType(storeInstance.getType());
        if (storeType == null) {
            return DEFAULT_VECTOR_STORE_MAX_DIMENSION;
        }
        return VECTOR_STORE_MAX_DIMENSIONS.getOrDefault(storeType, DEFAULT_VECTOR_STORE_MAX_DIMENSION);
    }
}
