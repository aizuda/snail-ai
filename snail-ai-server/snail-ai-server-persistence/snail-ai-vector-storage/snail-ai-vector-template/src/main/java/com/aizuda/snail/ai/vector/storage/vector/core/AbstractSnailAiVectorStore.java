package com.aizuda.snail.ai.vector.storage.vector.core;

import com.aizuda.snail.ai.vector.storage.exception.VectorStoreException;
import com.aizuda.snail.ai.vector.storage.vector.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.*;

/**
 * 向量存储抽象：写入与检索均委托给 Spring AI {@code VectorStore}（由子类按 indexName 缓存），
 * 不在此层做 embedding；由 Spring AI 在 add / similaritySearch 内部完成向量化。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
public abstract class AbstractSnailAiVectorStore implements SnailAiVectorStore {

    protected final EmbeddingModel springAiEmbeddingModel;
    protected final Integer embeddingDimensions;

    protected AbstractSnailAiVectorStore(EmbeddingModel springAiEmbeddingModel,
                                         Integer embeddingDimensions) {
        this.springAiEmbeddingModel = springAiEmbeddingModel;
        this.embeddingDimensions = embeddingDimensions;
    }

    @Override
    public void add(VectorAddRequest request) {
        if (request == null || request.getDocuments() == null || request.getDocuments().isEmpty()) {
            return;
        }
        if (request.getIndexName() == null || request.getIndexName().isBlank()) {
            throw new VectorStoreException("indexName 不能为空");
        }

        List<Document> documents = toDocuments(request.getDocuments());
        getVectorStore(request.getIndexName()).add(documents);
        log.info("向量库写入 {} 个文档 index={}", documents.size(), request.getIndexName());
    }

    @Override
    public void delete(String indexName, List<String> ids) {
        getVectorStore(indexName).delete(ids);
        log.info("向量库删除 {} 个文档 index={}", ids == null ? 0 : ids.size(), indexName);
    }

    @Override
    public List<VectorSearchResult> search(VectorSearchRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }
        if (request.getIndexName() == null || request.getIndexName().isBlank()) {
            throw new VectorStoreException("indexName 不能为空");
        }
        if (request.getQueryText() == null || request.getQueryText().isBlank()) {
            return Collections.emptyList();
        }

        SearchRequest.Builder sb = SearchRequest.builder()
                .query(request.getQueryText())
                .filterExpression(request.getFilterExpression())
                .topK(request.getTopK());
        List<Document> hits = getVectorStore(request.getIndexName()).similaritySearch(sb.build());
        List<VectorSearchResult> results = new ArrayList<>(hits.size());
        for (Document doc : hits) {
            Map<String, Object> meta = new HashMap<>(doc.getMetadata());
            float score = doc.getScore() != null ? doc.getScore().floatValue() : 0f;
            results.add(VectorSearchResult.builder()
                    .id(doc.getId())
                    .content(doc.getText())
                    .score(score)
                    .metadata(meta)
                    .build());
        }
        log.debug("向量库检索完成 index={} topK={} 命中 {} 条",
                request.getIndexName(), request.getTopK(), results.size());
        return results;
    }

    /**
     * 获取指定索引/集合对应的 Spring AI {@link VectorStore}（由子类按 indexName 缓存复用）。
     *
     * @param indexName 索引/集合名
     * @return 对应的 Spring AI 向量存储实例
     */
    protected abstract VectorStore getVectorStore(String indexName);

    private static List<Document> toDocuments(List<VectorDocument> documents) {
        List<Document> out = new ArrayList<>(documents.size());
        for (VectorDocument vd : documents) {
            Map<String, Object> meta = new HashMap<>();
            if (vd.getMetadata() != null) {
                meta.putAll(vd.getMetadata());
            }
            out.add(Document.builder()
                    .id(vd.getId())
                    .text(vd.getContent() != null ? vd.getContent() : "")
                    .metadata(meta)
                    .build());
        }
        return out;
    }
}
