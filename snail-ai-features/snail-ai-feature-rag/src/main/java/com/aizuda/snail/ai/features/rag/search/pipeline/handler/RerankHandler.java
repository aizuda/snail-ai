package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchHandler;
import com.aizuda.snail.ai.features.rag.strategy.rerank.RerankService;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

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

        if (ctx.getMetrics() != null) {
            ctx.getMetrics().setRerankMs(System.currentTimeMillis() - start);
        }
    }
}
