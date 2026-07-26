package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import cn.hutool.core.collection.CollUtil;
import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.common.util.RagResultReorderUtil;
import com.aizuda.snail.ai.features.rag.enums.RagChunkSourceTypeEnum;
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

/**
 * 收尾节点：截断 TopK、回填切片正文与文档名、按需扩展相邻切片，并做「迷失中间」重排。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Component
@Order(70)
@RequiredArgsConstructor
public class FinalizeHandler implements RagSearchHandler {

    private final RagChunkMapper ragChunkMapper;
    private final RagDocumentMapper ragDocumentMapper;
    private final SearchResultImageEnricher searchResultImageEnricher;

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
        searchResultImageEnricher.enrich(results, ctx.getQuery());

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

    /**
     * 为每条结果扩展前后相邻切片，拼接为更完整的上下文内容。
     */
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
            if (RagChunkSourceTypeEnum.isImage(currentChunk.getSourceType())) {
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


    /**
     * 批量回填结果的切片正文、文档 ID 与文档名（仅填充缺失字段）。
     */
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
        if (CollUtil.isNotEmpty(docIds)) {
            // 批量查询文档名，避免在循环中逐条 selectById 造成 N+1
            for (RagDocumentPO doc : ragDocumentMapper.selectByIds(docIds)) {
                docNameMap.put(doc.getId(), doc.getName());
            }
        }

        for (SearchResult result : results) {
            if (result.getChunkId() == null) {
                continue;
            }
            RagChunkPO chunk = chunkMap.get(result.getChunkId());
            if (chunk == null) {
                continue;
            }
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
