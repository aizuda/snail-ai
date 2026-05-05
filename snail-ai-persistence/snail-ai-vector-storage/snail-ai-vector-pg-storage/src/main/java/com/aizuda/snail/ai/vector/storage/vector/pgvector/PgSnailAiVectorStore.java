package com.aizuda.snail.ai.vector.storage.vector.pgvector;

import com.aizuda.snail.ai.common.context.SnailSpringContext;
import com.aizuda.snail.ai.vector.storage.enums.VectorStoreType;
import com.aizuda.snail.ai.vector.storage.exception.VectorStoreException;
import com.aizuda.snail.ai.vector.storage.vector.core.AbstractSnailAiVectorStore;
import com.aizuda.snail.ai.model.model.embedding.SnailEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreSQL：单表 {@code vector_store} + metadata 区分 RAG / 记忆分区；
 * 语义删除按 {@code ragId} / {@code agentId+userId} 过滤，不再使用旧表 {@code snail_knowledge_chunk_vector}。
 */
@Slf4j
public class PgSnailAiVectorStore extends AbstractSnailAiVectorStore {

    private final JdbcTemplate jdbcTemplate;
    private final PgVectorSettings config;

    public PgSnailAiVectorStore(SnailEmbeddingModel snailEmbeddingModel,
                                Integer embeddingDimensions,
                                PgVectorSettings config) {
        super(snailEmbeddingModel, embeddingDimensions);
        this.jdbcTemplate = SnailSpringContext.getBean(JdbcTemplate.class);
        this.config = config;
    }

    private PgVectorStore getStore(String indexName) {
        int dim = embeddingDimensions != null ? embeddingDimensions : config.getDefaultDimension();
        var builder = PgVectorStore.builder(jdbcTemplate, springAiEmbeddingModel)
                .initializeSchema(true)
                .dimensions(dim)
                .schemaName("public")
                .vectorTableName(indexName);
        var store = builder.build();
        try {
            store.afterPropertiesSet();
        } catch (Exception e) {
            throw new VectorStoreException("PgVectorStore（Spring AI）初始化失败", e);
        }
        return store;
    }

    @Override
    public String getType() {
        return VectorStoreType.PG_VECTOR.getType();
    }


    @Override
    public void deleteByIndexName(String indexName) {
        if (indexName == null || indexName.isBlank()) {
            throw new VectorStoreException("indexName 不能为空");
        }

        jdbcTemplate.execute("drop table " + indexName);
    }

    @Override
    public boolean test() {

        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            return conn.isValid(5);
        } catch (Exception e) {
            log.warn("PostgreSQL 连接测试失败: {}", e.getMessage());
            return false;
        }
    }


    @Override
    protected VectorStore getVectorStore(String indexName) {
        return getStore(indexName);
    }

}
