package com.aizuda.snail.ai.model.builder;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.common.model.RerankApiClient;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通用 Rerank 工厂实现
 * 兼容 Jina Reranker、Cohere Rerank、BGE Reranker 等标准 rerank HTTP 接口
 *
 * 标准接口格式:
 * POST {baseUrl}/rerank
 * Authorization: Bearer {apiKey}
 * { "model": "...", "query": "...", "documents": [...], "top_n": N }
 */
@Slf4j
@Component
public class OpenAiRerankModelFactory implements RerankModelFactory {

    @Override
    public String getSupportedProvider() {
        return "openai";
    }

    @Override
    public RerankApiClient createRerankClient(String providerKey, String baseUrl, String apiKey,
                                               String modelKey, ConfigExtAttrsDTO config) throws Exception {
        if (!isConfigValid(baseUrl, apiKey)) {
            throw new ModelCallException(ModelCallException.ErrorCode.INVALID_PARAMETER,
                    "baseUrl 和 apiKey 不能为空");
        }

        // 解析 configJson 获取自定义 rerank 路径（默认 /rerank）和超时
        String rerankPath = "/rerank";
        long readTimeoutMs = 60_000L;
        if (config.getRerankPath() != null && !config.getRerankPath().isBlank()) {
            rerankPath = config.getRerankPath();
        }
        if (config.getTimeoutMs() != null && config.getTimeoutMs() > 0) {
            readTimeoutMs = config.getTimeoutMs();
        }

        String fullUrl = baseUrl.replaceAll("/+$", "") + rerankPath;

        log.debug("Creating RerankApiClient for provider: {}, model: {}, url: {}", providerKey, modelKey, fullUrl);

        return new HttpRerankApiClient(fullUrl, apiKey, modelKey, readTimeoutMs);
    }

    /**
     * 基于 HTTP 的 Rerank API 客户端
     */
    static class HttpRerankApiClient implements RerankApiClient {

        private final String url;
        private final String apiKey;
        private final String modelKey;
        private final RestClient restClient;

        HttpRerankApiClient(String url, String apiKey, String modelKey, long timeoutMs) {
            this.url = url;
            this.apiKey = apiKey;
            this.modelKey = modelKey;
            long connectTimeoutMs = Math.min(timeoutMs, 10_000L);
            org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory =
                    new org.springframework.http.client.SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
            requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
            this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<RerankResultItem> rerank(String query, List<String> documents, int topN) {
            if (documents == null || documents.isEmpty()) {
                return List.of();
            }
            int n = Math.min(Math.max(topN, 1), documents.size());
            RerankParams params = new RerankParams(modelKey, new RerankOptions(query, documents, n));
            log.debug("rerank {}", params);
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(params)
                    .retrieve()
                    .body(Map.class);

            log.info("rerank response: {}", JsonUtil.toJson(response));
            if (response == null) {
                log.warn("Rerank API returned null body");
                return List.of();
            }
            if (response.containsKey("error")) {
                log.error("Rerank API error: {}", response.get("error"));
                return List.of();
            }
            JsonNode json = JsonUtil.toJson(response);
            // 兼容 output.results（如阿里云）和顶层 results（如 Jina/Cohere）两种格式
            JsonNode results = json.path("output").path("results");
            if (results.isMissingNode() || !results.isArray()) {
                results = json.path("results");
            }
            if (results.isMissingNode() || !results.isArray() || results.isEmpty()) {
                log.warn("Rerank API returned unexpected response (no results): {}", response.keySet());
                return List.of();
            }

            List<RerankResultItem> items = new ArrayList<>(results.size());
            for (JsonNode o : results) {
                JsonNode idxObj = o.get("index");
                int index = idxObj.asInt();
                double score = extractScore(o);
                items.add(new RerankResultItem(index, score));
            }
            return items;
        }

        /** Cohere: relevance_score；部分兼容接口使用 score */
        private static double extractScore(JsonNode r) {
            JsonNode rel = r.get("relevance_score");
            if (Objects.nonNull(rel)) {
                return rel.doubleValue();
            }
            JsonNode sc = r.get("score");
            if (Objects.nonNull(sc)) {
                return sc.doubleValue();
            }
            return 0.0;
        }
    }

    public record RerankParams(String model, RerankOptions input) {

    }

    public record RerankOptions(String query, List<String> documents, int top_n){}
}
