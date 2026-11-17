package com.aizuda.snail.ai.search.storage.search.postgresql;

import com.aizuda.snail.ai.common.Lifecycle;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.search.storage.search.SearchEngineFactory;
import com.aizuda.snail.ai.search.storage.search.enums.SearchEngineEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 注册并释放 PostgreSQL 全文检索引擎。 */
@Slf4j
@Component
public class PgFullTextSearchLifecycle implements Lifecycle {

    @Override
    public void start() {
        SearchEngineFactory.REGISTER.put(SearchEngineEnum.PG_FULLTEXT, config -> {
            PgFullTextSettings settings = config.getConfig() == null || config.getConfig().isBlank()
                    ? new PgFullTextSettings()
                    : JsonUtil.parseObject(config.getConfig(), PgFullTextSettings.class);
            return new PostgreSqlSearchEngine(settings);
        });
        log.info("PostgreSQL 全文检索引擎已注册");
    }

    @Override
    public void close() {
        PgFullTextDataSourceFactory.closeAll();
    }
}
