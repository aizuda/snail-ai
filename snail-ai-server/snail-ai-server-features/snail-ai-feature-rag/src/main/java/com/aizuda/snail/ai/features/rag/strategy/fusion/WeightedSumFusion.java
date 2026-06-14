package com.aizuda.snail.ai.features.rag.strategy.fusion;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Weighted Sum Fusion: final_score = alpha * vector_score + beta * bm25_score
 */
public class WeightedSumFusion implements HybridFusion {

    private final double alpha;
    private final double beta;

    public WeightedSumFusion(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public List<SearchResult> fuse(List<SearchResult> vectorResults,
                                   List<SearchResult> bm25Results,
                                   int outputSize) {
        Map<Long, Double> vectorScoreMap = new HashMap<>();
        Map<Long, Double> bm25ScoreMap = new HashMap<>();
        Map<Long, SearchResult> resultMap = new HashMap<>();

        double maxVec = vectorResults.stream().mapToDouble(SearchResult::getScore).max().orElse(1.0);
        double maxBm25 = bm25Results.stream().mapToDouble(SearchResult::getScore).max().orElse(1.0);

        for (SearchResult r : vectorResults) {
            vectorScoreMap.put(r.getChunkId(), maxVec > 0 ? r.getScore() / maxVec : 0);
            resultMap.putIfAbsent(r.getChunkId(), r);
        }

        for (SearchResult r : bm25Results) {
            bm25ScoreMap.put(r.getChunkId(), maxBm25 > 0 ? r.getScore() / maxBm25 : 0);
            resultMap.putIfAbsent(r.getChunkId(), r);
        }

        Map<Long, Double> finalScores = new HashMap<>();
        for (Long chunkId : resultMap.keySet()) {
            double vecScore = vectorScoreMap.getOrDefault(chunkId, 0.0);
            double bm25Score = bm25ScoreMap.getOrDefault(chunkId, 0.0);
            finalScores.put(chunkId, alpha * vecScore + beta * bm25Score);
        }

        return finalScores.entrySet().stream()
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
