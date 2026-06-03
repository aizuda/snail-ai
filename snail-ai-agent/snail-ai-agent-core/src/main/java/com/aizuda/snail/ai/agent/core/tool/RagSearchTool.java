package com.aizuda.snail.ai.agent.core.tool;

import com.aizuda.snail.ai.agent.common.context.AgentChatContextHolder;
import com.aizuda.snail.ai.agent.common.rpc.RpcClient;
import com.aizuda.snail.ai.common.dto.rag.RagSearchRequest;
import com.aizuda.snail.ai.common.dto.rag.RagSearchResponse;
import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.*;

/**
 * 客户端本地 RAG 知识库搜索工具
 * <p>
 * 通过 gRPC 回调服务端执行检索，支持多知识库（由 LLM 通过 ragId 参数选择）。
 */
@Slf4j
public class RagSearchTool {

    private final List<Long> ragIds;
    private final RpcClient rpcClient;

    public RagSearchTool(List<Long> ragIds, RpcClient rpcClient) {
        this.ragIds = ragIds;
        this.rpcClient = rpcClient;
    }

    @Tool(name = "rag_search",
            description = "Search a specific knowledge base by ragId. "
                    + "Use the ragId parameter to specify which knowledge base to search. "
                    + "Call this tool when the user's question may require professional knowledge or domain-specific information.")
    public String search(
            @ToolParam(description = "The ID of the knowledge base to search") Long ragId,
            @ToolParam(description = "The user's question or related query") String queryQuestion) {

        if (queryQuestion == null || queryQuestion.trim().isEmpty()) {
            return "No relevant reference materials found";
        }

        log.info("rag_search: ragId={}, query={}", ragId, queryQuestion);

        try {
            AgentChatContextHolder.ChatContext chatCtx = AgentChatContextHolder.getContext();

            RagSearchRequest req = RagSearchRequest.builder()
                    .ragId(ragId)
                    .query(queryQuestion.trim())
                    .parentObservationId(chatCtx != null ? chatCtx.getCurrentToolObservationId() : null)
                    .build();

            RagSearchResponse resp = rpcClient.searchRag(req);
            List<SearchResult> results = resp != null ? resp.getResults() : null;

            if (results == null || results.isEmpty()) {
                return "未找到相关参考资料。";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                sb.append(String.format("[%d] %s\n\n", i + 1, results.get(i).getContent()));
            }
            log.info("rag_search: ragId={}, result={}", ragId, sb);
            return sb.toString().trim();

        } catch (Exception e) {
            log.error("rag_search failed, ragId={}", ragId, e);
            return "搜索失败: " + e.getMessage();
        }
    }

}
