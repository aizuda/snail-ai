package com.aizuda.snail.ai.features.rag.strategy.fusion;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion: score = 1 / (k + rank)
 */
public class RRFFusion implements HybridFusion {

    private final int k;

    public RRFFusion(int k) {
        this.k = k;
    }

    @Override
    public List<SearchResult> fuse(List<SearchResult> vectorResults,
                                   List<SearchResult> bm25Results,
                                   int outputSize) {
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, SearchResult> resultMap = new HashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResult r = vectorResults.get(i);
            scores.merge(r.getChunkId(), 1.0 / (k + i + 1), Double::sum);
            resultMap.putIfAbsent(r.getChunkId(), r);
        }

        for (int i = 0; i < bm25Results.size(); i++) {
            SearchResult r = bm25Results.get(i);
            scores.merge(r.getChunkId(), 1.0 / (k + i + 1), Double::sum);
            resultMap.putIfAbsent(r.getChunkId(), r);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(outputSize)
                .map(e -> SearchResult.builder()
                        .chunkId(e.getKey())
                        .content(resultMap.get(e.getKey()).getContent())
                        .score(e.getValue())
                        .build())
                .toList();
    }
}
