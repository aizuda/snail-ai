package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchHandler;
import com.aizuda.snail.ai.features.rag.strategy.rerank.RerankService;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 重排节点：启用重排且配置了重排模型时，对候选结果调用重排服务取 TopN。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
public class RerankHandler implements RagSearchHandler {

    private final RerankService rerankService;

    @Override
    public void handle(RagSearchContext ctx) {
        RagConfigDO.SearchParams sp = ctx.getSearchParams();
        Long rerankModelId = sp.getRerankModelId() != null
                ? sp.getRerankModelId() : ctx.getKnowledge().getRerankModelId();

        if (!Boolean.TRUE.equals(sp.getRerankEnabled()) || rerankModelId == null) {
            log.debug("RAG 重排未启用或未配置重排模型，跳过 ragId={}", ctx.getRagId());
            return;
        }

        long start = System.currentTimeMillis();

        int enterRerankCount = sp.getEnterRerankCount();
        int resultCount = sp.getResultCount();

        List<SearchResult> sorted = ctx.getResults().stream()
                .sorted(Comparator.comparing(SearchResult::getScore).reversed())
                .toList();
        List<SearchResult> rerankInput = sorted.stream()
                .limit(Math.max(1, enterRerankCount))
                .toList();
        int rerankOutputTopN = Math.min(Math.max(1, resultCount), rerankInput.size());

        ctx.setResults(rerankService.rerank(ctx.getQuery(), rerankInput, rerankOutputTopN, rerankModelId));

        long cost = System.currentTimeMillis() - start;
        if (ctx.getMetrics() != null) {
            ctx.getMetrics().setRerankMs(cost);
        }
        log.debug("RAG 重排完成 输入 {} 条 输出 {} 条 耗时 {}ms modelId={}",
                rerankInput.size(), ctx.getResults().size(), cost, rerankModelId);
    }
}
