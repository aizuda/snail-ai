package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchHandler;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class QueryRewriteHandler implements RagSearchHandler {

    @Override
    public void handle(RagSearchContext ctx) {
        // 查询改写功能已移除
    }
}
