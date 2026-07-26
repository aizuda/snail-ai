package com.aizuda.snail.ai.features.rag.strategy.fusion;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;

import java.util.List;

/**
 * 混合检索结果融合策略：将向量检索与 BM25 检索结果按算法融合为统一排序列表。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface HybridFusion {

    /**
     * 融合向量与 BM25 结果，返回按融合分数倒序的前 {@code outputSize} 条。
     *
     * @param vectorResults 向量检索结果
     * @param bm25Results   BM25 检索结果
     * @param outputSize    输出条数上限
     * @return 融合并排序后的结果
     */
    List<SearchResult> fuse(List<SearchResult> vectorResults,
                            List<SearchResult> bm25Results,
                            int outputSize);
}
