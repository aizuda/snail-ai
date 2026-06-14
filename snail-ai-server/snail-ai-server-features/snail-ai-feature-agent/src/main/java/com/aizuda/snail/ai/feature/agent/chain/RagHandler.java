package com.aizuda.snail.ai.feature.agent.chain;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.common.enums.agent.RagCallModeEnum;
import com.aizuda.snail.ai.features.rag.dto.RagSearchRequestDTO;
import com.aizuda.snail.ai.features.rag.dto.RagSearchResponseDTO;
import com.aizuda.snail.ai.features.rag.service.RagSearchService;
import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * RAG 检索：若智能体绑定了知识库，根据调用方式处理
 * - 强制调用：服务端虚拟线程并行检索所有知识库，结果注入 systemPrompt
 * - 智能调用：注入知识库信息到 prompt，由 LLM 通过 tool 决定是否调用
 */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
public class RagHandler implements AgentChatHandler {

    private final RagMapper ragMapper;
    private final RagSearchService ragSearchService;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) {
            return;
        }
        if (!Boolean.TRUE.equals(ctx.getAgent().getRagEnabled())) {
            return;
        }

        String ragIdsStr = ctx.getAgent().getRagIds();
        if (StrUtil.isBlank(ragIdsStr)) {
            return;
        }

        List<Long> ragIds = Arrays.stream(ragIdsStr.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(Long::parseLong)
                .toList();

        if (ragIds.isEmpty()) {
            return;
        }

        List<RagPO> ragList = ragMapper.selectBatchIds(ragIds);
        if (ragList.isEmpty()) {
            log.warn("知识库不存在, ragIds={}", ragIdsStr);
            return;
        }

        RagCallModeEnum callMode = RagCallModeEnum.of(ctx.getAgent().getRagCallMode());

        if (callMode == RagCallModeEnum.FORCED) {
            // 强制调用：服务端直接并行检索所有知识库，结果注入 prompt
            String retrievedContent = parallelSearch(ragIds, ctx.getContent());
            ctx.setSystemPrompt(ctx.getSystemPrompt() + buildForcedPrompt(ragList, retrievedContent));
        } else {
            // 智能调用：注入可用知识库信息，由 LLM 通过 rag_search tool 决定
            ctx.setSystemPrompt(ctx.getSystemPrompt() + buildSmartPrompt(ragList));
        }
    }

    /**
     * 虚拟线程并行检索多个知识库
     */
    private String parallelSearch(List<Long> ragIds, String query) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<CompletableFuture<List<SearchResult>>> futures = ragIds.stream()
                .map(ragId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        RagSearchRequestDTO req = new RagSearchRequestDTO();
                        req.setRagId(ragId);
                        req.setQuery(query);
                        RagSearchResponseDTO resp = ragSearchService.search(req);
                        return resp.getResults() != null ? resp.getResults() : List.<SearchResult>of();
                    } catch (Exception e) {
                        log.warn("RAG并行检索失败, ragId={}", ragId, e);
                        return List.<SearchResult>of();
                    }
                }, executor))
                .toList();

        List<SearchResult> allResults = new ArrayList<>();
        for (CompletableFuture<List<SearchResult>> future : futures) {
            try {
                allResults.addAll(future.join());
            } catch (Exception e) {
                log.warn("RAG检索结果获取失败", e);
            }
        }
        executor.close();

        if (allResults.isEmpty()) {
            return "（未找到相关参考资料）";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allResults.size(); i++) {
            sb.append(String.format("[%d] %s\n\n", i + 1, allResults.get(i).getContent()));
        }
        return sb.toString().trim();
    }

    /**
     * 强制调用模式的 prompt：直接给出检索结果
     */
    private String buildForcedPrompt(List<RagPO> ragList, String retrievedContent) {
        String names = ragList.stream().map(RagPO::getName).collect(Collectors.joining(", "));
        StringBuilder sb = new StringBuilder("\n\n## Knowledge Base Reference\n\n");
        sb.append("The following are relevant materials retrieved from knowledge bases (");
        sb.append(names);
        sb.append("):\n\n");
        sb.append(retrievedContent);
        sb.append("\n\nPlease answer the user's question based on the above reference materials.\n");
        return sb.toString();
    }

    /**
     * 智能调用模式的 prompt：列出可用知识库，由 LLM 通过 tool 决定
     */
    private String buildSmartPrompt(List<RagPO> ragList) {
        StringBuilder sb = new StringBuilder("\n\n## Available Knowledge Bases\n\n");
        sb.append("Use `rag_search(ragId, query)` tool to search:\n\n");
        for (RagPO rag : ragList) {
            sb.append("- **ragId=").append(rag.getId()).append("** | ").append(rag.getName());
            if (StrUtil.isNotBlank(rag.getDescription())) {
                sb.append(" — ").append(rag.getDescription());
            }
            sb.append("\n");
        }
        sb.append("\n**Guidelines**: Call rag_search when the user's question relates to the knowledge base topics above. ");
        sb.append("Do not call for general chat or greetings.\n");
        return sb.toString();
    }
}
