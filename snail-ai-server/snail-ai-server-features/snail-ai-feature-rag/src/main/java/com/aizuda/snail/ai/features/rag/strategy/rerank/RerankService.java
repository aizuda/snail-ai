package com.aizuda.snail.ai.features.rag.strategy.rerank;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;

import java.util.List;

public interface RerankService {

    /**
     * 对搜索结果进行重排
     *
     * @param query          查询文本
     * @param candidates     候选结果列表（调用方已按「进入重排数量」截断）
     * @param rerankOutputTopN 重排后保留条数（与 UI「结果返回数量」一致，实际为 min(该值, candidates.size())）
     * @param rerankModelId  重排模型配置 ID
     * @return 重排后的结果列表
     */
    List<SearchResult> rerank(String query, List<SearchResult> candidates,
                              int rerankOutputTopN, Long rerankModelId);
}
