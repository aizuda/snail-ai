package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.persistence.rag.mapper.RagMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RAG 检索：若智能体绑定了知识库，检索相关文档并追加至 systemPrompt
 */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
public class RagHandler implements AgentChatHandler {
    private final RagMapper ragMapper;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) {
            return;
        }
        if (!Boolean.TRUE.equals(ctx.getAgent().getRagEnabled())) {
            return;
        }

        Long ragId = ctx.getAgent().getRagId();
        if (ragId == null || ragId <= 0) {
            return;
        }

        RagPO ragPO = ragMapper.selectById(ragId);

        if (ragPO == null) {
            log.warn("知识库不存在, ragId={}", ragId);
            return;
        }

        String knowledgePrompt = buildKnowledgePrompt(ragPO);
        ctx.setSystemPrompt(ctx.getSystemPrompt() + knowledgePrompt);
    }

    private String buildKnowledgePrompt(RagPO knowledge) {
        StringBuilder sb = new StringBuilder("\n\n## Available Knowledge Base\n\n");
        sb.append("You can use the `rag_search` tool:\n\n");
        sb.append("**ragId**: ").append(knowledge.getId()).append("\n");
        sb.append("**Name**: ").append(knowledge.getName()).append("\n");

        if (knowledge.getDescription() != null && !knowledge.getDescription().isBlank()) {
            sb.append("**Description**: ").append(knowledge.getDescription()).append("\n");
        }

        sb.append("\n**Usage Guidelines**:\n");
        sb.append("- When the user asks about professional knowledge or document content related to [").append(knowledge.getName()).append("], call the rag_search tool\n");
        sb.append("- Use keywords relevant to the user's question as query parameters\n");
        sb.append("- For general chat, greetings, or questions clearly outside the knowledge base scope, do not call it\n");
        sb.append("### Example Workflow\n");
        sb.append("### Use `rag_search(\"user's question\")` to retrieve knowledge base content.\n");

        return sb.toString();
    }
}
