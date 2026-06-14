package com.aizuda.snail.ai.features.rag.strategy.rerank;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.common.model.RerankApiClient;
import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.model.service.ModelRuntimeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 默认重排服务实现
 * 通过模型运行入口进行真正的 cross-encoder 重排。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRerankService implements RerankService {

    private final ModelRuntimeHandler modelRuntimeHandler;

    @Override
    public List<SearchResult> rerank(String query, List<SearchResult> candidates,
                                     int rerankOutputTopN, Long rerankModelId) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        if (rerankModelId == null) {
            log.warn("rerankModelId is null, falling back to score-based sorting");
            return sortByScore(candidates, rerankOutputTopN);
        }

        try {
            List<String> documents = candidates.stream()
                    .map(SearchResult::getContent)
                    .map(c -> c != null ? c : "")
                    .toList();

            List<RerankApiClient.RerankResultItem> results = modelRuntimeHandler.rerank(
                    new ModelRuntimeHandler.RerankRequest(rerankModelId, query, documents, rerankOutputTopN));
            if (results.isEmpty()) {
                log.warn("Rerank API returned no results, falling back to original ordering by score");
                return sortByScore(candidates, rerankOutputTopN);
            }

            List<SearchResult> reranked = new ArrayList<>(results.size());
            for (RerankApiClient.RerankResultItem result : results) {
                if (result.index() >= 0 && result.index() < candidates.size()) {
                    SearchResult dto = candidates.get(result.index());
                    dto.setScore(result.score());
                    reranked.add(dto);
                }
            }
            if (reranked.isEmpty()) {
                return sortByScore(candidates, rerankOutputTopN);
            }
            return reranked;

        } catch (ModelCallException e) {
            log.error("Rerank failed: {}", e.getMessage());
            return sortByScore(candidates, rerankOutputTopN);

        } catch (Exception e) {
            log.error("Rerank failed, falling back to score-based sorting", e);
            return sortByScore(candidates, rerankOutputTopN);
        }
    }

    private List<SearchResult> sortByScore(List<SearchResult> candidates, int topN) {
        return candidates.stream()
                .sorted(Comparator.comparing(SearchResult::getScore).reversed())
                .limit(topN)
                .toList();
    }
}
