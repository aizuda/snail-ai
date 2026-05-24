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
 * 通过 gRPC 回调服务端执行检索，在客户端直接访问 AgentChatContextHolder
 * 获取实时观测性 ID，并发布 RETRIEVER 观测事件。
 */
@Slf4j
public class RagSearchTool {

    private final Long ragId;
    private final RpcClient rpcClient;

    public RagSearchTool(Long ragId, RpcClient rpcClient) {
        this.ragId = ragId;
        this.rpcClient = rpcClient;
    }

    @Tool(name = "rag_search",
            description = "Retrieve relevant reference materials from the knowledge base. "
                    + "Call this tool when the user's question may require professional knowledge, document content, or domain-specific information. "
                    + "Use keywords relevant to the user's question as query parameters. "
                    + "Do not call for general chat, greetings, or questions clearly outside the knowledge base scope.")
    public String search(
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
            return sb.toString().trim();

        } catch (Exception e) {
            log.error("rag_search failed, ragId={}", ragId, e);
            return "搜索失败: " + e.getMessage();
        }
    }

}
