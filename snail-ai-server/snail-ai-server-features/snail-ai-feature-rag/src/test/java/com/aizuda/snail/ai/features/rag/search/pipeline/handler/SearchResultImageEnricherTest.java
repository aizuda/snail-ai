package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import java.lang.reflect.Proxy;
import java.util.List;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentImageMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SearchResultImageEnricherTest {

    private static final String CONTEXT_REASON = "关联到命中的文本片段";
    private static final String CERTIFICATE_NO = "合格证编号";
    private static final String CERTIFICATE_LIST = "合格证列表";
    private static final String LOGIN_PAGE = "登录页面";
    private static final String USER_PASSWORD = "用户名 密码";

    @Test
    void enrichMarksOcrMatchedImageAsDirectMatch() {
        RagDocumentImageMapper mapper = mapperReturning(List.of(RagDocumentImagePO.builder()
                .id(1L)
                .chunkId(10L)
                .imageUrl("/resource/certificate.png")
                .caption("图 5 证书列表")
                .ocrText(CERTIFICATE_NO + " 有效期 发证日期")
                .sourcePage(7)
                .imageIndex(1)
                .build()));
        SearchResultImageEnricher enricher = new SearchResultImageEnricher(mapper);
        SearchResult result = SearchResult.builder()
                .chunkId(10L)
                .content(CERTIFICATE_NO)
                .build();

        enricher.enrich(List.of(result), CERTIFICATE_NO + "在哪里");

        assertNotNull(result.getImages());
        assertEquals(1, result.getImages().size());
        assertEquals("DIRECT_MATCH", result.getImages().get(0).getMatchType());
        assertEquals("ocrText", result.getImages().get(0).getMatchedFields().get(0));
        assertEquals("OCR 命中：" + CERTIFICATE_NO, result.getImages().get(0).getMatchReason());
    }

    @Test
    void springContextCreatesEnricherWithDependencyConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                SpringConstructorTestConfig.class, SearchResultImageEnricher.class)) {
            assertNotNull(context.getBean(SearchResultImageEnricher.class));
        }
    }

    @Test
    void enrichReturnsDirectImagesBeforeContextImagesWhenChunkHasMultipleImages() {
        RagDocumentImageMapper mapper = mapperReturning(List.of(
                RagDocumentImagePO.builder()
                        .id(1L)
                        .chunkId(10L)
                        .imageUrl("/resource/context.png")
                        .caption("图 6 " + LOGIN_PAGE)
                        .figureTitle(LOGIN_PAGE)
                        .ocrText(USER_PASSWORD)
                        .sourcePage(1)
                        .imageIndex(1)
                        .build(),
                RagDocumentImagePO.builder()
                        .id(2L)
                        .chunkId(10L)
                        .imageUrl("/resource/direct.png")
                        .caption("图 5 " + CERTIFICATE_LIST)
                        .figureTitle(CERTIFICATE_LIST)
                        .ocrText(CERTIFICATE_NO + " 状态")
                        .sourcePage(2)
                        .imageIndex(1)
                        .build()
        ));
        SearchResultImageEnricher enricher = new SearchResultImageEnricher(mapper);
        SearchResult result = SearchResult.builder()
                .chunkId(10L)
                .content(CERTIFICATE_NO + "\n" + USER_PASSWORD)
                .build();

        enricher.enrich(List.of(result), CERTIFICATE_NO);

        assertNotNull(result.getImages());
        assertEquals(2, result.getImages().size());
        assertEquals("/resource/direct.png", result.getImages().get(0).getImageUrl());
        assertEquals("DIRECT_MATCH", result.getImages().get(0).getMatchType());
        assertEquals("/resource/context.png", result.getImages().get(1).getImageUrl());
        assertEquals("CONTEXT_MATCH", result.getImages().get(1).getMatchType());
    }

    @Test
    void enrichMarksImageAsContextMatchWhenNoImageEvidenceMatchesQuery() {
        RagDocumentImageMapper mapper = mapperReturning(List.of(RagDocumentImagePO.builder()
                .id(1L)
                .chunkId(10L)
                .imageUrl("/resource/login.png")
                .caption("图 6 " + LOGIN_PAGE)
                .figureTitle(LOGIN_PAGE)
                .ocrText(USER_PASSWORD)
                .sourcePage(2)
                .imageIndex(1)
                .build()));
        SearchResultImageEnricher enricher = new SearchResultImageEnricher(mapper);
        SearchResult result = SearchResult.builder()
                .chunkId(10L)
                .content(LOGIN_PAGE)
                .build();

        enricher.enrich(List.of(result), CERTIFICATE_NO);

        assertNotNull(result.getImages());
        assertEquals(1, result.getImages().size());
        assertEquals("CONTEXT_MATCH", result.getImages().get(0).getMatchType());
        assertEquals(CONTEXT_REASON, result.getImages().get(0).getMatchReason());
        assertEquals(List.of(), result.getImages().get(0).getMatchedTerms());
    }

    @Test
    void enrichDoesNotTreatDocumentNameAsDirectMatch() {
        RagDocumentImageMapper mapper = mapperReturning(List.of(RagDocumentImagePO.builder()
                .id(1L)
                .chunkId(10L)
                .imageUrl("/resource/login.png")
                .documentName(CERTIFICATE_LIST + ".pdf")
                .caption("图 6 " + LOGIN_PAGE)
                .figureTitle(LOGIN_PAGE)
                .ocrText(USER_PASSWORD)
                .build()));
        SearchResultImageEnricher enricher = new SearchResultImageEnricher(mapper);
        SearchResult result = SearchResult.builder()
                .chunkId(10L)
                .content(CERTIFICATE_LIST)
                .build();

        enricher.enrich(List.of(result), CERTIFICATE_NO);

        assertNotNull(result.getImages());
        assertEquals(1, result.getImages().size());
        assertEquals("CONTEXT_MATCH", result.getImages().get(0).getMatchType());
        assertEquals(List.of(), result.getImages().get(0).getMatchedFields());
    }

    @Test
    void enrichSortsDirectImagesBeforeContextImagesAndLimitsToThree() {
        RagDocumentImageMapper mapper = mapperReturning(List.of(
                RagDocumentImagePO.builder().id(1L).chunkId(10L).imageUrl("/context-1.png")
                        .caption(LOGIN_PAGE).sourcePage(1).imageIndex(1).build(),
                RagDocumentImagePO.builder().id(2L).chunkId(10L).imageUrl("/direct-1.png")
                        .caption(CERTIFICATE_LIST).sourcePage(2).imageIndex(1).build(),
                RagDocumentImagePO.builder().id(3L).chunkId(10L).imageUrl("/context-2.png")
                        .caption("用户中心").sourcePage(3).imageIndex(1).build(),
                RagDocumentImagePO.builder().id(4L).chunkId(10L).imageUrl("/direct-2.png")
                        .ocrText(CERTIFICATE_NO).sourcePage(4).imageIndex(1).build()
        ));
        SearchResultImageEnricher enricher = new SearchResultImageEnricher(mapper);
        SearchResult result = SearchResult.builder()
                .chunkId(10L)
                .content(CERTIFICATE_NO)
                .build();

        enricher.enrich(List.of(result), CERTIFICATE_NO);

        assertNotNull(result.getImages());
        assertEquals(3, result.getImages().size());
        assertEquals("DIRECT_MATCH", result.getImages().get(0).getMatchType());
        assertEquals("DIRECT_MATCH", result.getImages().get(1).getMatchType());
        assertEquals("CONTEXT_MATCH", result.getImages().get(2).getMatchType());
    }

    @Test
    void enrichMarksImagesAsContextMatchWhenQueryTermsAreEmpty() {
        RagDocumentImageMapper mapper = mapperReturning(List.of(RagDocumentImagePO.builder()
                .id(1L)
                .chunkId(10L)
                .imageUrl("/resource/certificate.png")
                .caption(CERTIFICATE_NO)
                .sourcePage(1)
                .imageIndex(1)
                .build()));
        SearchResultImageEnricher enricher = new SearchResultImageEnricher(mapper);
        SearchResult result = SearchResult.builder()
                .chunkId(10L)
                .content(CERTIFICATE_NO)
                .build();

        enricher.enrich(List.of(result), " ");

        assertNotNull(result.getImages());
        assertEquals(1, result.getImages().size());
        assertEquals("CONTEXT_MATCH", result.getImages().get(0).getMatchType());
        assertEquals(List.of(), result.getImages().get(0).getMatchedFields());
    }

    private static RagDocumentImageMapper mapperReturning(List<RagDocumentImagePO> images) {
        return (RagDocumentImageMapper) Proxy.newProxyInstance(
                RagDocumentImageMapper.class.getClassLoader(),
                new Class[]{RagDocumentImageMapper.class},
                (proxy, method, args) -> {
                    if ("selectList".equals(method.getName())) {
                        return images;
                    }
                    return null;
                });
    }

    @Configuration
    static class SpringConstructorTestConfig {

        @Bean
        RagDocumentImageMapper ragDocumentImageMapper() {
            return mapperReturning(List.of());
        }

        @Bean
        ImageEvidenceMatcher imageEvidenceMatcher() {
            return new ImageEvidenceMatcher();
        }
    }
}
