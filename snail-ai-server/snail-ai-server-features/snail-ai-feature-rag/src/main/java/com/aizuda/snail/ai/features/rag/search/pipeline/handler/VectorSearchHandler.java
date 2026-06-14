package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchHandler;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.vector.storage.vector.VectorStoreFactory;
import com.aizuda.snail.ai.vector.storage.vector.api.IndexNameBuilder;
import com.aizuda.snail.ai.vector.storage.vector.api.SnailAiVectorStore;
import com.aizuda.snail.ai.vector.storage.vector.api.VectorSearchRequest;
import com.aizuda.snail.ai.vector.storage.vector.api.VectorSearchResult;
import cn.hutool.core.util.NumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(30)
@RequiredArgsConstructor
public class VectorSearchHandler implements RagSearchHandler {
    private final VectorStoreFactory vectorStoreFactory;

    @Override
    public void handle(RagSearchContext ctx) {
        long start = System.currentTimeMillis();

        RagConfigDO.SearchParams searchParams = ctx.getSearchParams();
        SnailAiVectorStore vectorStore = vectorStoreFactory.create(ctx.getKnowledge());
        List<VectorSearchResult> raw = vectorStore.search(VectorSearchRequest.builder()
                .indexName(IndexNameBuilder.KNOWLEDGE.build(Map.of("ragId", ctx.getKnowledge().getId())))
                .queryText(ctx.getQuery())
                .topK(searchParams.getResultCount() * 2)
                .build());

        List<SearchResult> vectorResults = raw.stream()
                .map(r -> toDTO(r, r.getMetadata()))
                .toList();

        vectorResults = filterByThreshold(ctx.getSearchParams(), vectorResults);

        ctx.setVectorResults(vectorResults);
        ctx.setResults(vectorResults);

        if (ctx.getMetrics() != null) {
            ctx.getMetrics().setVectorSearchMs(System.currentTimeMillis() - start);
            ctx.getMetrics().setVectorHitCount(vectorResults.size());
        }
    }

    private SearchResult toDTO(VectorSearchResult r, Map<String, Object> meta) {
        Long chunkId = extractChunkId(r.getId(), meta);
        return SearchResult.builder()
                .chunkId(chunkId)
                .content(r.getContent())
                .score(r.getScore())
                .metadata(meta)
                .build();
    }

    private Long extractChunkId(String id, Map<String, Object> meta) {
        if (meta != null && meta.containsKey("chunkId")) {
            return NumberUtil.parseLong(String.valueOf(meta.get("chunkId")));
        }
        try { return Long.parseLong(id); } catch (NumberFormatException e) { return null; }
    }

    static List<SearchResult> filterByThreshold(RagConfigDO.SearchParams sp, List<SearchResult> list) {
        if (Boolean.TRUE.equals(sp.getThresholdEnabled()) && sp.getThreshold() != null && sp.getThreshold() > 0) {
            double threshold = sp.getThreshold();
            list = list.stream().filter(r -> r.getScore() >= threshold).toList();
        }
        return list;
    }
}
