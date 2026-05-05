package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.common.util.RagResultReorderUtil;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchHandler;
import com.aizuda.snail.ai.persistence.rag.mapper.RagChunkMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagChunkPO;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(70)
@RequiredArgsConstructor
public class FinalizeHandler implements RagSearchHandler {

    private final RagChunkMapper ragChunkMapper;
    private final RagDocumentMapper ragDocumentMapper;

    @Override
    public void handle(RagSearchContext ctx) {
        int topK = ctx.getSearchParams().getResultCount();
        List<SearchResult> results = ctx.getResults().stream().limit(topK).toList();
        enrichWithContent(results);

        // 扩展相邻切片
        Integer nearbySliceCount = ctx.getModelParams().getNearbySliceCount() ;
        if (Objects.nonNull(nearbySliceCount) && nearbySliceCount > 0) {
            results = expandNearbySlices(results, nearbySliceCount);
        }

        results = RagResultReorderUtil.reorderForLostInTheMiddle(results);
        ctx.setResults(results);

        if (ctx.getMetrics() != null) {
            ctx.getMetrics().setFinalCount(results.size());
        }

        log.info("RAG search: query='{}', ragId={}, vec={}, bm25={}, final={}, ms={}",
                ctx.getQuery(), ctx.getRagId(),
                ctx.getMetrics() != null ? ctx.getMetrics().getVectorHitCount() : 0,
                ctx.getMetrics() != null ? ctx.getMetrics().getBm25HitCount() : 0,
                results.size(),
                ctx.getMetrics() != null ? ctx.getMetrics().getTotalMs() : 0);
    }

    private List<SearchResult> expandNearbySlices(List<SearchResult> results, int nearbyCount) {
        List<SearchResult> expanded = new ArrayList<>();
        for (SearchResult result : results) {
            if (result.getChunkId() == null) {
                expanded.add(result);
                continue;
            }
            RagChunkPO currentChunk = ragChunkMapper.selectById(result.getChunkId());
            if (currentChunk == null) {
                expanded.add(result);
                continue;
            }
            List<RagChunkPO> nearbyChunks = ragChunkMapper.selectList(
                    new LambdaQueryWrapper<RagChunkPO>()
                            .eq(RagChunkPO::getDocumentId, currentChunk.getDocumentId())
                            .between(RagChunkPO::getChunkIndex,
                                    Math.max(0, currentChunk.getChunkIndex() - nearbyCount),
                                    currentChunk.getChunkIndex() + nearbyCount)
                            .orderByAsc(RagChunkPO::getChunkIndex)
            );
            result.setContent(nearbyChunks.stream()
                    .map(RagChunkPO::getContent)
                    .collect(Collectors.joining("\n")));
            expanded.add(result);
        }
        return expanded;
    }


    private void enrichWithContent(List<SearchResult> results) {
        List<Long> chunkIds = results.stream()
                .map(SearchResult::getChunkId)
                .filter(Objects::nonNull)
                .toList();
        if (chunkIds.isEmpty()) {
            return;
        }

        Map<Long, RagChunkPO> chunkMap = ragChunkMapper.selectByIds(chunkIds).stream()
                .collect(Collectors.toMap(RagChunkPO::getId, c -> c));

        Set<Long> docIds = chunkMap.values().stream()
                .map(RagChunkPO::getDocumentId)
                .collect(Collectors.toSet());
        Map<Long, String> docNameMap = new HashMap<>();
        if (!docIds.isEmpty()) {
            for (Long docId : docIds) {
                RagDocumentPO doc = ragDocumentMapper.selectById(docId);
                if (doc != null) {
                    docNameMap.put(docId, doc.getName());
                }
            }
        }

        for (SearchResult result : results) {
            if (result.getChunkId() == null) continue;
            RagChunkPO chunk = chunkMap.get(result.getChunkId());
            if (chunk == null) continue;
            if (result.getContent() == null) {
                result.setContent(chunk.getContent());
            }
            if (result.getDocumentId() == null) {
                result.setDocumentId(chunk.getDocumentId());
            }
            if (result.getDocumentName() == null && chunk.getDocumentId() != null) {
                result.setDocumentName(docNameMap.get(chunk.getDocumentId()));
            }
        }
    }
}
