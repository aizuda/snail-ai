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
        log.debug("RAG 检索链路开始 ragId={} 节点数={}", ctx.getRagId(), handlers.size());
        for (RagSearchHandler handler : handlers) {
            if (ctx.isTerminated()) {
                log.debug("RAG 检索链路已终止，跳出剩余节点 ragId={}", ctx.getRagId());
                break;
            }
            handler.handle(ctx);
        }
        long totalMs = System.currentTimeMillis() - startTime;
        if (ctx.getMetrics() != null) {
            ctx.getMetrics().setTotalMs(totalMs);
        }
        log.debug("RAG 检索链路结束 ragId={} 总耗时 {}ms", ctx.getRagId(), totalMs);
        return ctx;
    }
}
