package com.aizuda.snail.ai.features.rag.search.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG 检索责任链编排器：按 {@code @Order} 顺序依次执行各 {@link RagSearchHandler}，
 * 任一节点标记终止即跳出，并统计总耗时。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagSearchPipeline {

    private final List<RagSearchHandler> handlers;

    /**
     * 顺序执行检索链路；任一节点终止则中断，最后写入总耗时并返回上下文。
     */
    public RagSearchContext execute(RagSearchContext ctx) {
        long startTime = System.currentTimeMillis();
        for (RagSearchHandler handler : handlers) {
            if (ctx.isTerminated()) {
                break;
            }
            handler.handle(ctx);
        }
        if (ctx.getMetrics() != null) {
            ctx.getMetrics().setTotalMs(System.currentTimeMillis() - startTime);
        }
        return ctx;
    }
}
