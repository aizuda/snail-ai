package com.aizuda.snail.ai.features.rag.strategy.rerank;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.model.Model;
import com.aizuda.snail.ai.model.model.ModelFactory;
import com.aizuda.snail.ai.model.model.rerank.RerankModel;
import com.aizuda.snail.ai.model.model.rerank.RerankResponse;
import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 默认重排服务实现
 * 通过 ModelFactory 获取 RerankModel 进行真正的 cross-encoder 重排
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRerankService implements RerankService {

    private final ModelFactory modelFactory;

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
            Model model = modelFactory.getModel(rerankModelId);
            if (!(model instanceof RerankModel rerankModel)) {
                log.error("Model {} is not a RerankModel", rerankModelId);
                return sortByScore(candidates, rerankOutputTopN);
            }

            // 提取候选文档的 content
            List<String> documents = candidates.stream()
                    .map(SearchResult::getContent)
                    .map(c -> c != null ? c : "")
                    .toList();

            RerankResponse response = rerankModel.rerank(
                    new RerankModel.RerankDTO(query, documents, rerankOutputTopN));

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                log.warn("Rerank API returned no results, falling back to original ordering by score");
                return sortByScore(candidates, rerankOutputTopN);
            }

            // 根据返回的 index + score 映射回 SearchResultDTO（按 API 返回顺序）
            List<SearchResult> reranked = new ArrayList<>(response.getResults().size());
            for (RerankResponse.RerankResult result : response.getResults()) {
                if (result.getIndex() >= 0 && result.getIndex() < candidates.size()) {
                    SearchResult dto = candidates.get(result.getIndex());
                    dto.setScore(result.getScore());
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
