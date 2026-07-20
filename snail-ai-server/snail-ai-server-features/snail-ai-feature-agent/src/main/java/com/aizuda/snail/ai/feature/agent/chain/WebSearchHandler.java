package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.common.websearch.WebSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

/**
 * 联网搜索处理器：若智能体启用了 webSearch，将 WebSearchTool 注入工具回调列表，
 * 由 LLM 在需要时自主决策是否调用。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-14
 */
@Slf4j
//@Component
//@Order(65)
public class WebSearchHandler implements AgentChatHandler {

    @Value("${snail-ai.web-search.tavily.api-key:}")
    private String tavilyApiKey;

    @Value("${snail-ai.web-search.max-results:5}")
    private int maxResults;

    @Override
    public void handle(AgentChatContext ctx) {
        if (!Boolean.TRUE.equals(ctx.getAgent().getWebSearchEnabled())) {
            return;
        }
        if (tavilyApiKey == null || tavilyApiKey.isBlank()) {
            log.warn("webSearchEnabled=true 但未配置 snail-ai.web-search.tavily.api-key，跳过联网搜索, agentId={}", ctx.getAgentId());
            return;
        }

        // 仅追加新工具回调；不能把已有回调整体再加回去，否则 ctx 中的回调会重复注册
        List<ToolCallback> webSearchCallbacks =
                Arrays.asList(ToolCallbacks.from(new WebSearchTool(tavilyApiKey, maxResults)));
        ctx.getToolCallbacks().addAll(webSearchCallbacks);
        log.debug("联网搜索工具已注入, agentId={}, maxResults={}", ctx.getAgentId(), maxResults);
    }
}
