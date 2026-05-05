package com.aizuda.snail.ai.vector.storage.vector.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.Version;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.aizuda.snail.ai.vector.storage.enums.VectorStoreType;
import com.aizuda.snail.ai.vector.storage.exception.VectorStoreException;
import com.aizuda.snail.ai.vector.storage.vector.core.AbstractSnailAiVectorStore;
import com.aizuda.snail.ai.model.model.embedding.SnailEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.ai.vectorstore.filter.Filter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.*;

@Slf4j
public class ElasticsearchSnailAiVectorStore extends AbstractSnailAiVectorStore {

    private final static String INDEX_NAME = "test_index";
    private final ElasticsearchVectorSettings config;
    private final Rest5Client rest5Client;
    private final ConcurrentHashMap<String, org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore> cache =
            new ConcurrentHashMap<>();

    public ElasticsearchSnailAiVectorStore(SnailEmbeddingModel snailEmbeddingModel,
                                           Integer embeddingDimensions,
                                           ElasticsearchVectorSettings config) {
        super(snailEmbeddingModel, embeddingDimensions);
        this.config = config;
        this.rest5Client = buildRest5Client(config);
    }

    @Override
    public String getType() {
        return VectorStoreType.ELASTICSEARCH.getType();
    }

    private ElasticsearchVectorStore getStore(String indexName) {
        return cache.computeIfAbsent(indexName, idx -> {
            ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
            options.setIndexName(idx);
            options.setDimensions(embeddingDimensions);
            options.setSimilarity(parseSimilarity(config.getSimilarity()));

            var store = ElasticsearchVectorStore
                    .builder(rest5Client, springAiEmbeddingModel)
                    .options(options)
                    .initializeSchema(true)
                    .batchingStrategy(new TokenCountBatchingStrategy())
                    .build();
            try {
                store.afterPropertiesSet();
            } catch (Exception e) {
                throw new VectorStoreException("ElasticsearchVectorStore 初始化失败: " + idx, e);
            }
            return store;
        });
    }

    private static SimilarityFunction parseSimilarity(String s) {
        if (s == null || s.isBlank()) {
            return SimilarityFunction.cosine;
        }
        try {
            return SimilarityFunction.valueOf(s.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            return SimilarityFunction.cosine;
        }
    }

    private static Rest5Client buildRest5Client(ElasticsearchVectorSettings config) {
        try {
            String uri = config.getScheme() + "://" + config.getHost() + ":" + config.getPort();
            var builder = Rest5Client.builder(URI.create(uri));
            if (config.getUsername() != null && !config.getUsername().isBlank()
                    && config.getPassword() != null) {
                String raw = config.getUsername() + ":" + config.getPassword();
                String b64 = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
                Header[] headers = new Header[]{new BasicHeader("Authorization", "Basic " + b64)};
                builder.setDefaultHeaders(headers);
            }
            return builder.build();
        } catch (Exception e) {
            throw new VectorStoreException("构建 Elasticsearch Rest5Client 失败", e);
        }
    }

    @Override
    public void delete(String indexName, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        if (indexName == null || indexName.isBlank()) {
            throw new VectorStoreException("indexName 不能为空");
        }
        getStore(indexName).delete(ids);
    }

    @Override
    public void deleteByIndexName(String indexName) {
        if (indexName == null || indexName.isBlank()) {
            throw new VectorStoreException("indexName 不能为空");
        }
        cache.remove(indexName);
        try {
            ElasticsearchClient elasticsearchClient = (ElasticsearchClient) getStore(indexName).getNativeClient().get();
            if (elasticsearchClient.indices().exists(e -> e.index(indexName)).value()) {
                elasticsearchClient.indices().delete(d -> d.index(indexName));
                log.info("Deleted Elasticsearch index: {}", indexName);
            }
        } catch (IOException e) {
            throw new VectorStoreException("Failed to delete ES index: " + indexName, e);
        }
    }

    @Override
    public boolean test() {
        String version = Version.VERSION == null ? "Unknown" : Version.VERSION.toString();
        ElasticsearchClient elasticsearchClient =
                new ElasticsearchClient(new Rest5ClientTransport(rest5Client, new Jackson3JsonpMapper()))
                        .withTransportOptions(t -> t.addHeader("user-agent", "spring-ai elastic-java/" + version));
        try {
            elasticsearchClient.info();
            if (elasticsearchClient.indices().exists(e -> e.index(INDEX_NAME)).value()) {
                elasticsearchClient.indices().delete(d -> d.index(INDEX_NAME));
            }
            return true;
        } catch (IOException e) {
            log.warn("Elasticsearch 连接测试失败: {}", e.getMessage());
            return false;
        }
    }


    @Override
    protected VectorStore getVectorStore(String indexName) {
        return getStore(indexName);
    }
}
