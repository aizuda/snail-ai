package com.aizuda.snail.ai.vector.storage.vector.milvus;

import com.aizuda.snail.ai.common.Lifecycle;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.vector.storage.enums.VectorStoreType;
import com.aizuda.snail.ai.vector.storage.vector.VectorStoreFactory;
import org.springframework.stereotype.Component;

/**
 * <p>
 *
 * </p>
 *
 * @author opensnail
 * @date 2026-04-02
 */
@Component
public class MilvusVectorLifecycle implements Lifecycle {

    @Override
    public void start() {
        VectorStoreFactory.REGISTER.put(VectorStoreType.MILVUS, configDTO ->
                new MilvusSnailAiVectorStore(
                        configDTO.getEmbeddingModel(),
                        configDTO.getDimensions(),
                        JsonUtil.parseObject(configDTO.getConfig(), MilvusVectorSettings.class)
                ));
    }

    @Override
    public void close() {

    }
}
