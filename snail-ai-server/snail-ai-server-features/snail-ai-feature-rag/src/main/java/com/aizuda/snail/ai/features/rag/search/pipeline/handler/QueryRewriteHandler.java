package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchHandler;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 查询改写节点（当前为空实现，改写功能已移除，保留以维持链路顺序）。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Component
@Order(20)
public class QueryRewriteHandler implements RagSearchHandler {

    @Override
    public void handle(RagSearchContext ctx) {
        // 查询改写功能已移除
    }
}
