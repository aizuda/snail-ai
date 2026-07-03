package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import java.lang.reflect.Proxy;
import java.util.List;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentImageMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SearchResultImageEnricherTest {

    private static final String CONTEXT_REASON = "\u5173\u8054\u5230\u547d\u4e2d\u7684\u6587\u672c\u7247\u6bb5";
    private static final String CERTIFICATE_NO = "\u5408\u683c\u8bc1\u7f16\u53f7";
    private static final String CERTIFICATE_LIST = "\u5408\u683c\u8bc1\u5217\u8868";
    private static final String LOGIN_PAGE = "\u767b\u5f55\u9875\u9762";
    private static final String USER_PASSWORD = "\u7528\u6237\u540d \u5bc6\u7801";

    @Test
    void enrichMarksOcrMatchedImageAsDirectMatch() {
        RagDocumentImageMapper mapper = mapperReturning(List.of(RagDocumentImagePO.builder()
                .id(1L)
                .chunkId(10L)
                .imageUrl("/resource/certificate.png")
                .caption("\u56fe 5 \u8bc1\u4e66\u5217\u8868")
                .ocrText(CERTIFICATE_NO + " \u6709\u6548\u671f \u53d1\u8bc1\u65e5\u671f")
                .sourcePage(7)
                .imageIndex(1)
                .build()));
        SearchResultImageEnricher enricher = new SearchResultImageEnricher(mapper);
        SearchResult result = SearchResult.builder()
                .chunkId(10L)
                .content(CERTIFICATE_NO)
                .build();

        enricher.enrich(List.of(result), CERTIFICATE_NO + "\u5728\u54ea\u91cc");

        assertNotNull(result.getImages());
        assertEquals(1, result.getImages().size());
        assertEquals("DIRECT_MATCH", result.getImages().get(0).getMatchType());
        assertEquals("ocrText", result.getImages().get(0).getMatchedFields().get(0));
        assertEquals("OCR \u547d\u4e2d\uff1a" + CERTIFICATE_NO, result.getImages().get(0).getMatchReason());
    }

    @Test
    void enrichReturnsDirectImagesBeforeContextImagesWhenChunkHasMultipleImages() {
        RagDocumentImageMapper mapper = mapperReturning(List.of(
                RagDocumentImagePO.builder()
                        .id(1L)
                        .chunkId(10L)
                        .imageUrl("/resource/context.png")
                        .caption("\u56fe 6 " + LOGIN_PAGE)
                        .figureTitle(LOGIN_PAGE)
                        .ocrText(USER_PASSWORD)
                        .sourcePage(1)
                        .imageIndex(1)
                        .build(),
                RagDocumentImagePO.builder()
                        .id(2L)
                        .chunkId(10L)
                        .imageUrl("/resource/direct.png")
                        .caption("\u56fe 5 " + CERTIFICATE_LIST)
                        .figureTitle(CERTIFICATE_LIST)
                        .ocrText(CERTIFICATE_NO + " \u72b6\u6001")
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
                .caption("\u56fe 6 " + LOGIN_PAGE)
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
                .caption("\u56fe 6 " + LOGIN_PAGE)
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
                        .caption("\u7528\u6237\u4e2d\u5fc3").sourcePage(3).imageIndex(1).build(),
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

    private RagDocumentImageMapper mapperReturning(List<RagDocumentImagePO> images) {
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
}
