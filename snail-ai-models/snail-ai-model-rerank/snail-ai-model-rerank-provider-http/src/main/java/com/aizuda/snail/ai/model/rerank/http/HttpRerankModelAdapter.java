package com.aizuda.snail.ai.model.rerank.http;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.model.RerankApiClient;
import com.aizuda.snail.ai.model.common.ModelAdapterDefaults;
import com.aizuda.snail.ai.model.rerank.RerankModelAdapter;
import com.aizuda.snail.ai.model.rerank.RerankModelSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class HttpRerankModelAdapter implements RerankModelAdapter, Ordered {

    private static final String DEFAULT_RERANK_PATH = "/rerank";
    private static final long DEFAULT_TIMEOUT_MS = 60_000L;
    private static final long MAX_CONNECT_TIMEOUT_MS = 10_000L;

    @Override
    public String adapterKey() {
        return ModelAdapterDefaults.HTTP_ADAPTER;
    }

    @Override
    public RerankApiClient create(RerankModelSpec spec) {
        ConfigExtAttrsDTO config = spec.configJson();
        String fullUrl = spec.baseUrl().replaceAll("/+$", "") + rerankPath(config);
        return new HttpRerankApiClient(fullUrl, spec.apiKey(), spec.modelKey(), readTimeoutMs(config));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private String rerankPath(ConfigExtAttrsDTO config) {
        if (config != null && config.getRerankPath() != null && !config.getRerankPath().isBlank()) {
            return config.getRerankPath();
        }
        return DEFAULT_RERANK_PATH;
    }

    private long readTimeoutMs(ConfigExtAttrsDTO config) {
        Long timeoutMs = config != null ? config.getTimeoutMs() : null;
        return timeoutMs != null && timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }

    static class HttpRerankApiClient implements RerankApiClient {

        private final String url;
        private final String apiKey;
        private final String modelKey;
        private final RestClient restClient;

        HttpRerankApiClient(String url, String apiKey, String modelKey, long timeoutMs) {
            this.url = url;
            this.apiKey = apiKey;
            this.modelKey = modelKey;
            long connectTimeoutMs = Math.min(timeoutMs, MAX_CONNECT_TIMEOUT_MS);
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
            int n = Math.clamp(topN, 1, documents.size());
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(new RerankParams(modelKey, query, documents, n))
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                log.warn("Rerank API returned null body");
                return List.of();
            }
            if (response.containsKey("error")) {
                log.error("Rerank API error: {}", response.get("error"));
                return List.of();
            }
            return parseResults(response);
        }

        private List<RerankResultItem> parseResults(Map<String, Object> response) {
            List<?> results = results(response);
            if (results == null || results.isEmpty()) {
                log.warn("Rerank API returned unexpected response (no results): {}", response.keySet());
                return List.of();
            }
            List<RerankResultItem> items = new ArrayList<>(results.size());
            for (Object item : results) {
                if (item instanceof Map<?, ?> result) {
                    items.add(new RerankResultItem(intValue(result.get("index")), score(result)));
                }
            }
            return items;
        }

        private List<?> results(Map<String, Object> response) {
            Object output = response.get("output");
            if (output instanceof Map<?, ?> outputMap && outputMap.get("results") instanceof List<?> list) {
                return list;
            }
            Object results = response.get("results");
            return results instanceof List<?> list ? list : null;
        }

        private double score(Map<?, ?> result) {
            Object relevanceScore = result.get("relevance_score");
            if (relevanceScore instanceof Number number) {
                return number.doubleValue();
            }
            Object score = result.get("score");
            return score instanceof Number number ? number.doubleValue() : 0.0D;
        }

        private int intValue(Object value) {
            return value instanceof Number number ? number.intValue() : 0;
        }
    }

    public record RerankParams(String model, String query, List<String> documents, int top_n) {
    }
}
