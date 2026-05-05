package com.aizuda.snail.ai.search.storage.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.search.storage.search.api.SearchAddRequest;
import com.aizuda.snail.ai.search.storage.search.api.SearchDocument;
import com.aizuda.snail.ai.search.storage.search.api.SearchDeleteRequest;
import com.aizuda.snail.ai.search.storage.search.api.SearchEngine;
import com.aizuda.snail.ai.search.storage.search.api.SearchRequest;
import com.aizuda.snail.ai.search.storage.search.exception.SearchEngineException;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ElasticsearchSearchEngine implements SearchEngine {

    private static final String ES_FIELD_CONTENT = "content";
    private static final String ES_FIELD_METADATA = "metadata";
    private static final int DEFAULT_TOP_K = 10;
    private final ElasticsearchClient client;

    public ElasticsearchSearchEngine(ElasticsearchVectorSettings config) {
        this.client = createClient(config);
    }

    @Override
    public void insert(SearchAddRequest request) {
        if (request == null || request.getDocuments() == null || request.getDocuments().isEmpty()) {
            return;
        }
        String indexName = request.getIndexName();
        if (indexName == null) {
            throw new SearchEngineException("SearchAddRequest.indexName 不能为空");
        }
        List<SearchDocument> documents = request.getDocuments().stream()
                .filter(document -> document != null && document.getId() != null && document.getContent() != null)
                .toList();
        if (documents.isEmpty()) {
            return;
        }
        batchInsert(indexName, documents);
    }

    @Override
    public List<SearchResult> search(SearchRequest request) {
        if (request == null) {
            return List.of();
        }
        String queryText = request.getQueryText();
        if (queryText == null || queryText.isBlank()) {
            return List.of();
        }

        String indexName = request.getIndexName();
        int topK = request.getTopK() > 0 ? request.getTopK() : DEFAULT_TOP_K;

        try {
            boolean exists = client.indices().exists(r -> r.index(indexName)).value();
            if (!exists) {
                return List.of();
            }

            Query matchQuery = Query.of(q -> q.match(m -> m.field(ES_FIELD_CONTENT).query(queryText)));
            String filterExpression = request.getFilterExpression();
            SearchResponse<Map> response;
            if (filterExpression != null && !filterExpression.isBlank()) {
                response = client.search(s -> s
                        .index(indexName)
                        .query(q -> q.bool(b -> b
                                .must(matchQuery)
                                .filter(Query.of(fl -> fl
                                        .queryString(qs -> qs.query(filterExpression))
                                ))
                        ))
                        .size(topK), Map.class);
            } else {
                response = client.search(s -> s
                        .index(indexName)
                        .query(matchQuery)
                        .size(topK), Map.class);
            }

            List<SearchResult> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) {
                    continue;
                }
                results.add(SearchResult.builder()
                        .id(hit.id())
                        .content((String) source.get(ES_FIELD_CONTENT))
                        .score(hit.score() != null ? hit.score().floatValue() : 0f)
                        .metadata(toMetadata(source))
                        .build());
            }
            return results;
        } catch (IOException e) {
            throw new SearchEngineException("Elasticsearch full-text search failed", e);
        }
    }

    @Override
    public void delete(SearchDeleteRequest request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return;
        }
        String indexName = request.getIndexName();
        if (indexName == null) {
            throw new SearchEngineException("SearchDeleteRequest.indexName 不能为空");
        }
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (String id : request.getIds()) {
                bulkBuilder.operations(op -> op
                        .delete(d -> d.index(indexName).id(id)));
            }
            client.bulk(bulkBuilder.build());
        } catch (IOException e) {
            throw new SearchEngineException("Failed to delete from Elasticsearch search index", e);
        }
    }

    private void batchInsert(String indexName, List<SearchDocument> documents) {
        ensureIndex(indexName);
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (SearchDocument document : documents) {
                Map<String, Object> source = toSource(document.getMetadata(), document.getContent());
                bulkBuilder.operations(op -> op
                        .index(idx -> idx.index(indexName).id(document.getId()).document(source)));
            }

            BulkResponse response = client.bulk(bulkBuilder.build());
            if (response.errors()) {
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        log.error("ES search engine bulk error: {} - {}", item.id(), item.error().reason());
                    }
                }
                throw new SearchEngineException("Elasticsearch bulk insert had errors");
            }
        } catch (IOException e) {
            throw new SearchEngineException("Failed to insert documents into Elasticsearch search index", e);
        }
    }

    private Map<String, Object> toSource(Map<String, Object> metadata, String content) {
        Map<String, Object> source = new HashMap<>();
        source.put(ES_FIELD_CONTENT, content);
        source.put(ES_FIELD_METADATA, metadata != null ? new HashMap<>(metadata) : new HashMap<>());
        return source;
    }

    private Map<String, Object> toMetadata(Map<String, Object> source) {
        Object metadataObj = source.get(ES_FIELD_METADATA);
        if (metadataObj instanceof Map<?, ?> metadataMap) {
            Map<String, Object> metadata = new HashMap<>();
            for (Map.Entry<?, ?> entry : metadataMap.entrySet()) {
                if (entry.getKey() != null) {
                    metadata.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return metadata;
        }
        return Map.of();
    }

    private void ensureIndex(String indexName) {
        try {
            boolean exists = client.indices().exists(r -> r.index(indexName)).value();
            if (!exists) {
                client.indices().create(r -> r
                        .index(indexName)
                        .settings(s -> s.analysis(a -> a
                                .analyzer("cjk_analyzer", an -> an.custom(c -> c
                                        .tokenizer("standard")
                                        .filter(List.of("cjk_bigram", "lowercase"))))))
                        .mappings(m -> m
                                .properties(ES_FIELD_CONTENT, Property.of(p -> p
                                        .text(TextProperty.of(tp -> tp.analyzer("cjk_analyzer")))))));
                log.info("Created ES search index: {}", indexName);
            }
        } catch (IOException e) {
            throw new SearchEngineException("Failed to ensure ES search index: " + indexName, e);
        }
    }

    private static ElasticsearchClient createClient(ElasticsearchVectorSettings config) {
        String uri = config.getScheme() + "://" + config.getHost() + ":" + config.getPort();
        var builder = Rest5Client.builder(URI.create(uri));
        if (config.getUsername() != null && !config.getUsername().isBlank()
                && config.getPassword() != null) {
            String raw = config.getUsername() + ":" + config.getPassword();
            String b64 = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            Header[] headers = new Header[]{new BasicHeader("Authorization", "Basic " + b64)};
            builder.setDefaultHeaders(headers);
        }
        Rest5Client rest5Client = builder.build();
        Rest5ClientTransport transport = new Rest5ClientTransport(rest5Client, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ElasticsearchVectorSettings settings;

        public Builder setConfig(com.aizuda.snail.ai.search.storage.search.elasticsearch.ElasticsearchVectorSettings settings) {
            this.settings = settings;
            return this;
        }

        public ElasticsearchSearchEngine build() {
            return new ElasticsearchSearchEngine(settings);
        }
    }
}
