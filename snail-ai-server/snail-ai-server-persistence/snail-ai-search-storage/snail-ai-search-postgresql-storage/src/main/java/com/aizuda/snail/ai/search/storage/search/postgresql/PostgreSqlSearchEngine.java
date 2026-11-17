package com.aizuda.snail.ai.search.storage.search.postgresql;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.search.storage.search.api.SearchAddRequest;
import com.aizuda.snail.ai.search.storage.search.api.SearchDeleteRequest;
import com.aizuda.snail.ai.search.storage.search.api.SearchDocument;
import com.aizuda.snail.ai.search.storage.search.api.SearchEngine;
import com.aizuda.snail.ai.search.storage.search.api.SearchRequest;
import com.aizuda.snail.ai.search.storage.search.exception.SearchEngineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.core.type.TypeReference;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 基于 PostgreSQL pg_trgm 的轻量全文检索实现。 */
@Slf4j
public class PostgreSqlSearchEngine implements SearchEngine {

    private static final String TABLE_NAME = "snail_ai_search_document";
    private static final String CONTENT_INDEX_NAME = "idx_snail_ai_search_document_content_trgm";
    private static final int DEFAULT_TOP_K = 10;

    private final JdbcTemplate jdbcTemplate;
    private final String qualifiedTableName;

    public PostgreSqlSearchEngine(PgFullTextSettings settings) {
        settings.validate();
        this.jdbcTemplate = PgFullTextDataSourceFactory.getOrCreate(settings);
        this.qualifiedTableName = settings.getSchema() + "." + TABLE_NAME;
        initializeSchema();
    }

    @Override
    public void insert(SearchAddRequest request) {
        if (request == null || request.getDocuments() == null || request.getDocuments().isEmpty()) {
            return;
        }
        String indexName = requireIndexName(request.getIndexName());
        List<SearchDocument> documents = request.getDocuments().stream()
                .filter(document -> document != null
                        && document.getId() != null
                        && document.getContent() != null)
                .toList();
        if (documents.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO " + qualifiedTableName + " "
                + "(index_name, document_id, content, metadata, create_dt, update_dt) "
                + "VALUES (?, ?, ?, CAST(? AS jsonb), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                + "ON CONFLICT (index_name, document_id) DO UPDATE SET "
                + "content = EXCLUDED.content, metadata = EXCLUDED.metadata, update_dt = CURRENT_TIMESTAMP";
        List<Object[]> batchArgs = new ArrayList<>(documents.size());
        for (SearchDocument document : documents) {
            batchArgs.add(new Object[]{
                    indexName,
                    document.getId(),
                    document.getContent(),
                    JsonUtil.toJsonString(document.getMetadata() != null ? document.getMetadata() : Map.of())
            });
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);
        log.info("PostgreSQL 全文检索写入 {} 个文档 index={}", documents.size(), indexName);
    }

    @Override
    public List<SearchResult> search(SearchRequest request) {
        if (request == null || request.getQueryText() == null || request.getQueryText().isBlank()) {
            return List.of();
        }
        if (request.getFilterExpression() != null && !request.getFilterExpression().isBlank()) {
            throw new SearchEngineException("PostgreSQL 全文检索暂不支持元数据过滤表达式");
        }
        String indexName = requireIndexName(request.getIndexName());
        String queryText = request.getQueryText().trim();
        int topK = request.getTopK() > 0 ? request.getTopK() : DEFAULT_TOP_K;
        String sql = "SELECT document_id, content, metadata, "
                + "GREATEST(similarity(content, ?), word_similarity(?, content)) AS score "
                + "FROM " + qualifiedTableName + " "
                + "WHERE index_name = ? AND (content % ? OR content ILIKE ? ESCAPE '\\') "
                + "ORDER BY score DESC, document_id ASC LIMIT ?";
        return jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> SearchResult.builder()
                        .id(resultSet.getString("document_id"))
                        .content(resultSet.getString("content"))
                        .metadata(parseMetadata(resultSet.getString("metadata")))
                        .score(normalizeScore(resultSet.getDouble("score")))
                        .build(),
                queryText,
                queryText,
                indexName,
                queryText,
                toContainsPattern(queryText),
                topK);
    }

    @Override
    public void delete(SearchDeleteRequest request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return;
        }
        String indexName = requireIndexName(request.getIndexName());
        String sql = "DELETE FROM " + qualifiedTableName + " WHERE index_name = ? AND document_id = ?";
        List<Object[]> batchArgs = request.getIds().stream()
                .filter(id -> id != null && !id.isBlank())
                .map(id -> new Object[]{indexName, id})
                .toList();
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
    }

    @Override
    public boolean test() {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            return connection.isValid(5);
        } catch (Exception e) {
            log.warn("PostgreSQL 全文检索连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    private void initializeSchema() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        } catch (Exception e) {
            throw new SearchEngineException("初始化 pg_trgm 失败，请确认数据库已安装扩展且当前用户有创建扩展权限", e);
        }
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + qualifiedTableName + " ("
                    + "index_name varchar(255) NOT NULL, "
                    + "document_id varchar(255) NOT NULL, "
                    + "content text NOT NULL, "
                    + "metadata jsonb NOT NULL DEFAULT '{}'::jsonb, "
                    + "create_dt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "update_dt timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "PRIMARY KEY (index_name, document_id))");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS " + CONTENT_INDEX_NAME
                    + " ON " + qualifiedTableName + " USING GIN (content gin_trgm_ops)");
        } catch (Exception e) {
            throw new SearchEngineException(
                    "初始化 PostgreSQL 全文检索表失败，请确认 schema 存在且当前用户有建表和建索引权限", e);
        }
    }

    private String requireIndexName(String indexName) {
        if (indexName == null || indexName.isBlank()) {
            throw new SearchEngineException("indexName 不能为空");
        }
        return indexName;
    }

    private String toContainsPattern(String queryText) {
        String escaped = queryText
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private Map<String, Object> parseMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Map.of();
        }
        return JsonUtil.parseObject(metadata, new TypeReference<>() {
        });
    }

    private double normalizeScore(double score) {
        return Math.max(0D, Math.min(score, 1D));
    }
}
