package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.search.storage.search.SearchEngineFactory;
import com.aizuda.snail.ai.search.storage.search.api.SearchEngine;
import com.aizuda.snail.ai.search.storage.search.api.SearchRequest;
import com.aizuda.snail.ai.search.storage.search.constant.SearchFilterKeys;
import com.aizuda.snail.ai.search.storage.search.constant.SearchMetadataKeys;
import com.aizuda.snail.ai.vector.storage.vector.api.IndexNameBuilder;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchHandler;
import cn.hutool.core.util.NumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * BM25 检索节点：启用搜索引擎时执行全文检索并按阈值过滤，结果写入上下文。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class Bm25SearchHandler implements RagSearchHandler {
    private final SearchEngineFactory searchEngineFactory;

    @Override
    public void handle(RagSearchContext ctx) {
        if (!Boolean.TRUE.equals(ctx.getKnowledge().getSearchEngineEnable())) {
            log.debug("RAG BM25 检索未启用，跳过 ragId={}", ctx.getKnowledge().getId());
            return;
        }

        long start = System.currentTimeMillis();
        RagConfigDO.SearchParams searchParams = ctx.getSearchParams();
        SearchEngine searchEngine = searchEngineFactory.forStoreInstance(ctx.getKnowledge().getSearchEngineInstanceId());
        String indexName = IndexNameBuilder.KNOWLEDGE.build(Map.of(SearchFilterKeys.RAG_ID, ctx.getKnowledge().getId()));
        List<SearchResult> raw = searchEngine.search(SearchRequest.builder()
                .indexName(indexName)
                .queryText(ctx.getQuery())
                .topK(searchParams.getResultCount() * 2)
                .build());

        List<SearchResult> bm25Results = raw.stream()
                .map(r -> toDTO(r, r.getMetadata()))
                .toList();

        bm25Results = VectorSearchHandler.filterByThreshold(ctx.getSearchParams(), bm25Results);
        ctx.setBm25Results(bm25Results);

        long cost = System.currentTimeMillis() - start;
        if (ctx.getMetrics() != null) {
            ctx.getMetrics().setBm25SearchMs(cost);
            ctx.getMetrics().setBm25HitCount(bm25Results.size());
        }
        log.debug("RAG BM25 检索命中 {} 条 耗时 {}ms ragId={}",
                bm25Results.size(), cost, ctx.getKnowledge().getId());
    }

    private SearchResult toDTO(SearchResult r, Map<String, Object> meta) {
        Long chunkId = extractChunkId(r.getId(), meta);
        return SearchResult.builder()
                .chunkId(chunkId)
                .content(r.getContent())
                .score(r.getScore())
                .metadata(meta)
                .build();
    }

    private Long extractChunkId(String id, Map<String, Object> meta) {
        if (meta != null && meta.containsKey(SearchMetadataKeys.CHUNK_ID)) {
            return NumberUtil.parseLong(String.valueOf(meta.get(SearchMetadataKeys.CHUNK_ID)));
        }
        try { return Long.parseLong(id); } catch (NumberFormatException e) { return null; }
    }
}
