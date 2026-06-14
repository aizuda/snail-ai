package com.aizuda.snail.ai.vector.storage.vector.elasticsearch;

import com.aizuda.snail.ai.common.Lifecycle;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.vector.storage.enums.VectorStoreType;
import com.aizuda.snail.ai.vector.storage.vector.VectorStoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 向量库生命周期管理
 *
 * @author opensnail
 * @date 2026-04-02
 */
@Slf4j
@Component
public class EsVectorLifecycle implements Lifecycle {

    @Override
    public void start() {
        VectorStoreFactory.REGISTER.put(VectorStoreType.ELASTICSEARCH, configDTO ->
                new ElasticsearchSnailAiVectorStore(
                        configDTO.getEmbeddingModel(),
                        configDTO.getDimensions(),
                        JsonUtil.parseObject(configDTO.getConfig(), ElasticsearchVectorSettings.class)
                ));
    }

    @Override
    public void close() {
        EsClientFactory.closeAll();
    }
}
