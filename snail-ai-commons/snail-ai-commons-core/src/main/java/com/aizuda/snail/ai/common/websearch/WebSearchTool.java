package com.aizuda.snail.ai.common.websearch;

import com.aizuda.snail.ai.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 联网搜索工具（基于 Tavily API），通过 Spring AI @Tool 注解注册为 LLM 可调用工具。
 * <p>由 WebSearchHandler 在运行时实例化并注入责任链上下文。
 */
@Slf4j
public class WebSearchTool {

    private static final String TAVILY_API_URL = "https://api.tavily.com/search";

    private final RestClient restClient = RestClient.create();
    private final String apiKey;
    private final int maxResults;

    public WebSearchTool(String apiKey, int maxResults) {
        this.apiKey = apiKey;
        this.maxResults = maxResults;
    }

    @Tool(description = "Search the internet for the latest information. Call this tool when the question involves current news, recent events, real-time data, or content beyond the model's knowledge cutoff date.")
    public String search(String query) {
        try {
            Map<String, Object> body = Map.of(
                    "api_key", apiKey,
                    "query", query,
                    "max_results", maxResults,
                    "include_raw_content", false
            );
            String raw = restClient.post()
                    .uri(TAVILY_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return formatResults(raw);
        } catch (Exception e) {
            log.warn("Web search failed, query='{}': {}", query, e.getMessage());
            return "Search failed: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String formatResults(String json) {
        try {
            Map<String, Object> resp = JsonUtil.parseObject(json, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            if (results == null || results.isEmpty()) {
                return "No relevant search results found.";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> r = results.get(i);
                sb.append(String.format("[%d] %s%n%s%n%s%n%n",
                        i + 1,
                        r.getOrDefault("title", ""),
                        r.getOrDefault("url", ""),
                        r.getOrDefault("content", "")));
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return json;
        }
    }
}
