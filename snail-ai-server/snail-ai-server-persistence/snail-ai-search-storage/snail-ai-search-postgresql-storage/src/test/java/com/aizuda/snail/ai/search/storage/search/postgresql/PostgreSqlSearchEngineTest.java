package com.aizuda.snail.ai.search.storage.search.postgresql;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.search.storage.search.api.SearchAddRequest;
import com.aizuda.snail.ai.search.storage.search.api.SearchDeleteRequest;
import com.aizuda.snail.ai.search.storage.search.api.SearchDocument;
import com.aizuda.snail.ai.search.storage.search.api.SearchRequest;
import com.aizuda.snail.ai.search.storage.search.exception.SearchEngineException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlSearchEngineTest {

    private static final String PRIMARY_INDEX = "knowledge-base-a";
    private static final String SECONDARY_INDEX = "knowledge-base-b";

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("snail_ai")
            .withUsername("snail_ai")
            .withPassword("snail_ai");

    private static PostgreSqlSearchEngine searchEngine;

    @BeforeAll
    static void setUp() {
        searchEngine = new PostgreSqlSearchEngine(settings());
    }

    @AfterAll
    static void tearDown() {
        PgFullTextDataSourceFactory.closeAll();
    }

    @Test
    void shouldSearchMultilingualShortTextAndReturnMetadata() {
        insert(PRIMARY_INDEX,
                document("zh", "PostgreSQL 支持中文知识库检索", Map.of("language", "zh")),
                document("en", "PostgreSQL lightweight full text search", Map.of("language", "en")),
                document("short", "AI", Map.of("language", "short")));

        List<SearchResult> chineseResults = search(PRIMARY_INDEX, "中文知识库", 10);
        List<SearchResult> englishResults = search(PRIMARY_INDEX, "lightweight", 10);
        List<SearchResult> shortResults = search(PRIMARY_INDEX, "AI", 10);

        assertEquals("zh", chineseResults.getFirst().getId());
        assertEquals("zh", chineseResults.getFirst().getMetadata().get("language"));
        assertEquals("en", englishResults.getFirst().getId());
        assertEquals("short", shortResults.getFirst().getId());
        assertTrue(chineseResults.stream().allMatch(result -> result.getScore() >= 0D && result.getScore() <= 1D));
    }

    @Test
    void shouldUpsertAndKeepIndexesIsolated() {
        insert(PRIMARY_INDEX, document("shared-id", "old content", Map.of("version", 1)));
        insert(SECONDARY_INDEX, document("shared-id", "secondary content", Map.of("version", 1)));
        insert(PRIMARY_INDEX, document("shared-id", "updated content", Map.of("version", 2)));

        List<SearchResult> updatedResults = search(PRIMARY_INDEX, "updated", 10);
        List<SearchResult> secondaryResults = search(SECONDARY_INDEX, "secondary", 10);

        assertEquals(1, updatedResults.size());
        assertEquals(2, updatedResults.getFirst().getMetadata().get("version"));
        assertEquals(1, secondaryResults.size());
        assertEquals("secondary content", secondaryResults.getFirst().getContent());
        assertTrue(search(PRIMARY_INDEX, "secondary", 10).isEmpty());
    }

    @Test
    void shouldDeleteOnlyFromRequestedIndex() {
        insert(PRIMARY_INDEX, document("delete-me", "isolated deletion document", Map.of()));
        insert(SECONDARY_INDEX, document("delete-me", "isolated deletion document", Map.of()));

        searchEngine.delete(SearchDeleteRequest.builder()
                .indexName(PRIMARY_INDEX)
                .ids(List.of("delete-me"))
                .build());

        assertTrue(search(PRIMARY_INDEX, "isolated deletion", 10).isEmpty());
        assertFalse(search(SECONDARY_INDEX, "isolated deletion", 10).isEmpty());
    }

    @Test
    void shouldRejectFilterExpression() {
        SearchRequest request = SearchRequest.builder()
                .indexName(PRIMARY_INDEX)
                .queryText("query")
                .topK(10)
                .filterExpression("metadata->>'language' = 'zh'")
                .build();

        SearchEngineException exception = assertThrows(SearchEngineException.class,
                () -> searchEngine.search(request));

        assertTrue(exception.getMessage().contains("不支持元数据过滤"));
    }

    @Test
    void shouldRejectInvalidSchema() {
        PgFullTextSettings invalidSettings = settings();
        invalidSettings.setSchema("public;drop table users");

        SearchEngineException exception = assertThrows(SearchEngineException.class,
                () -> new PostgreSqlSearchEngine(invalidSettings));

        assertTrue(exception.getMessage().contains("schema"));
    }

    @Test
    void shouldTestConnection() {
        assertNotNull(searchEngine);
        assertTrue(searchEngine.test());
    }

    private static PgFullTextSettings settings() {
        PgFullTextSettings settings = new PgFullTextSettings();
        settings.setHost(POSTGRESQL.getHost());
        settings.setPort(POSTGRESQL.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        settings.setDatabase(POSTGRESQL.getDatabaseName());
        settings.setUsername(POSTGRESQL.getUsername());
        settings.setPassword(POSTGRESQL.getPassword());
        return settings;
    }

    private static SearchDocument document(String id, String content, Map<String, Object> metadata) {
        return SearchDocument.builder()
                .id(id)
                .content(content)
                .metadata(metadata)
                .build();
    }

    private static void insert(String indexName, SearchDocument... documents) {
        searchEngine.insert(SearchAddRequest.builder()
                .indexName(indexName)
                .documents(List.of(documents))
                .build());
    }

    private static List<SearchResult> search(String indexName, String queryText, int topK) {
        return searchEngine.search(SearchRequest.builder()
                .indexName(indexName)
                .queryText(queryText)
                .topK(topK)
                .build());
    }
}
