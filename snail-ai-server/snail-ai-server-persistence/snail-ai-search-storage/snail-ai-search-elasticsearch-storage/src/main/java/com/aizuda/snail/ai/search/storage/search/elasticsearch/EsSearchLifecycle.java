package com.aizuda.snail.ai.search.storage.search.elasticsearch;

import com.aizuda.snail.ai.common.Lifecycle;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.search.storage.search.SearchEngineFactory;
import com.aizuda.snail.ai.search.storage.search.enums.SearchEngineEnum;
import org.springframework.stereotype.Component;

@Component
public class EsSearchLifecycle implements Lifecycle {

    @Override
    public void start() {
        SearchEngineFactory.REGISTER.put(SearchEngineEnum.ELASTICSEARCH, configDTO ->
                ElasticsearchSearchEngine.builder()
                        .setConfig(JsonUtil.parseObject(configDTO.getConfig(), ElasticsearchVectorSettings.class))
                        .build());
    }

    @Override
    public void close() {
    }
}
