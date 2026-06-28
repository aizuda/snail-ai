package com.aizuda.snail.ai.features.rag.search.pipeline;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.features.rag.dto.RagStageMetricsDTO;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagSearchContext {

    // ── 输入 ──
    private String originalQuery;
    private Long ragId;
    private boolean debug;

    // ── 配置（ConfigResolveHandler 填充） ──
    private RagPO knowledge;
    private RagConfigDO.SearchParams searchParams;
    private RagConfigDO.ModelParams modelParams;

    // ── 流转数据 ──
    private String query;
    private List<SearchResult> vectorResults;
    private List<SearchResult> bm25Results;
    private List<SearchResult> results;

    // ── 指标 ──
    private RagStageMetricsDTO metrics;

    // ── 控制 ──
    private boolean terminated;

    // ── 外部传入配置（可选） ──
    private RagConfigDO externalConfigDO;
}
