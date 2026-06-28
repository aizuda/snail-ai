package com.aizuda.snail.ai.features.rag.strategy.fusion;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;

import java.util.List;

/**
 * HybridFusion
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface HybridFusion {

    List<SearchResult> fuse(List<SearchResult> vectorResults,
                            List<SearchResult> bm25Results,
                            int outputSize);
}
