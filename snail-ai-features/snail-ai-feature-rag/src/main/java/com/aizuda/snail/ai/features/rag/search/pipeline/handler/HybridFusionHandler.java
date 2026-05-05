package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import com.aizuda.snail.ai.features.rag.enums.FusionStrategy;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchHandler;
import com.aizuda.snail.ai.features.rag.strategy.fusion.HybridFusion;
import com.aizuda.snail.ai.features.rag.strategy.fusion.RRFFusion;
import com.aizuda.snail.ai.features.rag.strategy.fusion.WeightedSumFusion;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(50)
@RequiredArgsConstructor
public class HybridFusionHandler implements RagSearchHandler {
    private static final double ALPHA = 0.5;
    private static final double BETA = 0.5;
    private static final int RRF_K = 60;

    @Override
    public void handle(RagSearchContext ctx) {
        if (ctx.getBm25Results() == null || ctx.getBm25Results().isEmpty()) {
            return;
        }

        long start = System.currentTimeMillis();

        RagConfigDO.SearchParams sp = ctx.getSearchParams();
        HybridFusion fusion = createFusion(sp.getFusionStrategy(), sp.getDenseWeight(), sp.getRrfK());
        ctx.setResults(fusion.fuse(ctx.getVectorResults(), ctx.getBm25Results(), sp.getResultCount()));

        if (ctx.getMetrics() != null) {
            ctx.getMetrics().setFusionMs(System.currentTimeMillis() - start);
        }
    }

    private HybridFusion createFusion(String strategy, Double denseWeight, Integer rrfK) {
        if (FusionStrategy.WEIGHTED_SUM.getStrategy().equalsIgnoreCase(strategy)) {
            double alpha = denseWeight != null ? denseWeight : ALPHA;
            double beta = denseWeight != null ? 1.0 - denseWeight : BETA;
            return new WeightedSumFusion(alpha, beta);
        }
        int k = rrfK != null ? rrfK : RRF_K;
        return new RRFFusion(k);
    }
}
