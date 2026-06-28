package com.aizuda.snail.ai.features.rag.search.pipeline;

/**
 * RAG 检索责任链节点：对 {@link RagSearchContext} 执行一个检索阶段
 * （配置解析、向量 / BM25 检索、融合、重排、收尾等）。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface RagSearchHandler {

    /**
     * 处理当前检索阶段；可通过 {@link RagSearchContext#setTerminated(boolean)} 提前终止链路。
     */
    void handle(RagSearchContext ctx);
}
