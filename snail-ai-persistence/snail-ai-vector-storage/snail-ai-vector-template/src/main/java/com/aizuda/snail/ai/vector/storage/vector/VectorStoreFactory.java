package com.aizuda.snail.ai.vector.storage.vector;

import com.aizuda.snail.ai.vector.storage.vector.api.SnailAiVectorStore;

import com.aizuda.snail.ai.common.enums.rag.StoreInstanceTypeEnum;
import com.aizuda.snail.ai.model.model.ModelFactory;
import com.aizuda.snail.ai.model.model.embedding.SnailEmbeddingModel;
import com.aizuda.snail.ai.persistence.admin.mapper.StoreInstanceMapper;
import com.aizuda.snail.ai.vector.storage.enums.VectorStoreType;
import com.aizuda.snail.ai.vector.storage.embedding.EmbeddingModelDimensionService;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import com.aizuda.snail.ai.persistence.admin.po.StoreInstancePO;
import com.aizuda.snail.ai.vector.storage.exception.VectorStoreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 向量库工厂：连接与索引参数均来自「存储实例」表 config JSON。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorStoreFactory {

    public static final Map<VectorStoreType, Function<VectorStoreConfigDTO, SnailAiVectorStore>> REGISTER = new HashMap<>();

    private final ModelFactory modelFactory;
    private final StoreInstanceMapper storeInstanceMapper;
    private final EmbeddingModelDimensionService embeddingModelDimensionService;

    public SnailAiVectorStore create(RagPO knowledge) {
        return createForStoreInstance(
                knowledge.getVectorStoreInstanceId(),
                knowledge.getEmbeddingModelId(),
                embeddingModelDimensionService.getEmbeddingDimension(knowledge.getEmbeddingModelId()));
    }


    /**
     * 按存储实例 ID + 嵌入模型创建向量库（知识库 / 记忆配置共用）
     */
    public SnailAiVectorStore createForStoreInstance(Long vectorStoreInstanceId, Long embeddingModelId, Integer dimensionOfVectorModel) {
        if (vectorStoreInstanceId == null) {
            throw new VectorStoreException("vectorStoreInstanceId 不能为空");
        }
        StoreInstancePO inst = storeInstanceMapper.selectById(vectorStoreInstanceId);
        if (inst == null) {
            throw new VectorStoreException("向量库实例不存在: " + vectorStoreInstanceId);
        }
        StoreInstanceTypeEnum typeEnum = StoreInstanceTypeEnum.fromType(inst.getType());
        if (typeEnum == null) {
            throw new VectorStoreException("不支持的向量库类型: " + inst.getType());
        }
        SnailEmbeddingModel model = (SnailEmbeddingModel) modelFactory.getModel(embeddingModelId);
        return REGISTER.get(VectorStoreType.valueOf(typeEnum.name())).apply(
                VectorStoreConfigDTO
                        .builder()
                        .config(inst.getConfig())
                        .dimensions(dimensionOfVectorModel)
                        .embeddingModel(model)
                        .build()
        );

    }

}
