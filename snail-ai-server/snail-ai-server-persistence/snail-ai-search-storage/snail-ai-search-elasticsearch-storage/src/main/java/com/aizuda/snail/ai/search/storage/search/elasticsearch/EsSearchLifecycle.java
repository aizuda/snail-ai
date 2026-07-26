package com.aizuda.snail.ai.search.storage.search.elasticsearch;

import com.aizuda.snail.ai.common.Lifecycle;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.search.storage.search.SearchEngineFactory;
import com.aizuda.snail.ai.search.storage.search.enums.SearchEngineEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 全文检索生命周期组件，应用启动时向 {@link SearchEngineFactory} 注册 ES 引擎构造器。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Component
public class EsSearchLifecycle implements Lifecycle {

    @Override
    public void start() {
        SearchEngineFactory.REGISTER.put(SearchEngineEnum.ELASTICSEARCH, configDTO ->
                ElasticsearchSearchEngine.builder()
                        .setConfig(JsonUtil.parseObject(configDTO.getConfig(), ElasticsearchVectorSettings.class))
                        .build());
        log.info("Elasticsearch 全文检索引擎已注册");
    }

    @Override
    public void close() {
    }
}
