package com.aizuda.snail.ai.features.rag.service;

import com.aizuda.snail.ai.features.rag.dto.RagSearchRequestDTO;
import com.aizuda.snail.ai.features.rag.dto.RagSearchResponseDTO;
import com.aizuda.snail.ai.features.rag.dto.RagStageMetricsDTO;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchPipeline;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * RAG 检索服务，构建检索上下文并驱动检索责任链（pipeline）返回结果与可选的阶段指标。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Service
@RequiredArgsConstructor
public class RagSearchService {

    private final RagSearchPipeline pipeline;

    /**
     * 按请求执行 RAG 检索（使用知识库自身配置）
     *
     * @param request 检索请求
     * @return 检索结果及可选的阶段指标
     */
    public RagSearchResponseDTO search(RagSearchRequestDTO request) {
        return search(request, null);
    }

    /**
     * 按请求执行 RAG 检索，可传入外部配置覆盖知识库默认配置
     *
     * @param request  检索请求
     * @param configDO 外部检索配置，为 {@code null} 时使用知识库自身配置
     * @return 检索结果及可选的阶段指标
     */
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
