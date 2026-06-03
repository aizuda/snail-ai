package com.aizuda.snail.ai.vector.storage.vector.pgvector;

import com.aizuda.snail.ai.common.Lifecycle;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.vector.storage.enums.VectorStoreType;
import com.aizuda.snail.ai.vector.storage.vector.VectorStoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PgVector 生命周期管理
 *
 * @author opensnail
 * @date 2026-04-02
 */
@Slf4j
@Component
public class PgVectorLifecycle implements Lifecycle {

    @Override
    public void start() {
        VectorStoreFactory.REGISTER.put(VectorStoreType.PG_VECTOR, configDTO ->
                new PgSnailAiVectorStore(
                        configDTO.getEmbeddingModel(),
                        configDTO.getDimensions(),
                        JsonUtil.parseObject(configDTO.getConfig(), PgVectorSettings.class)
                ));
    }

    @Override
    public void close() {
        PgDataSourceFactory.closeAll();
    }
}
