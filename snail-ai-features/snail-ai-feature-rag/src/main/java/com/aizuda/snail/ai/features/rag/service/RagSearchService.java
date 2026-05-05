package com.aizuda.snail.ai.features.rag.service;

import com.aizuda.snail.ai.features.rag.dto.RagSearchRequestDTO;
import com.aizuda.snail.ai.features.rag.dto.RagSearchResponseDTO;
import com.aizuda.snail.ai.features.rag.dto.RagStageMetricsDTO;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchPipeline;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RagSearchService {

    private final RagSearchPipeline pipeline;

    public RagSearchResponseDTO search(RagSearchRequestDTO request) {
        return search(request, null);
    }

    public RagSearchResponseDTO search(RagSearchRequestDTO request, RagConfigDO configDO) {
        RagSearchContext ctx = RagSearchContext.builder()
                .originalQuery(request.getQuery())
                .ragId(request.getRagId())
                .debug(Boolean.TRUE.equals(request.getDebug()))
                .externalConfigDO(configDO)
                .metrics(new RagStageMetricsDTO())
                .build();

        pipeline.execute(ctx);

        return RagSearchResponseDTO.builder()
                .results(ctx.getResults())
                .metrics(ctx.isDebug() ? ctx.getMetrics() : null)
                .build();
    }
}
