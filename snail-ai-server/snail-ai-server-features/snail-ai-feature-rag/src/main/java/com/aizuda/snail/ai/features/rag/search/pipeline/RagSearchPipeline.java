package com.aizuda.snail.ai.features.rag.search.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagSearchPipeline {

    private final List<RagSearchHandler> handlers;

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
