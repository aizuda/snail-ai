package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentImageMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageEvidenceRecallHandlerTest {

    private static final String CERTIFICATE_NO = "合格证编号";

    @Test
    void handleAddsImageEvidenceResultWhenOcrTextMatchesQuery() {
        ImageEvidenceRecallHandler handler = new ImageEvidenceRecallHandler(
                mapperReturning(List.of(RagDocumentImagePO.builder()
                        .id(1L)
                        .ragId(100L)
                        .documentId(200L)
                        .chunkId(300L)
                        .documentName("证照手册.pdf")
                        .imageUrl("/resource/certificate.png")
                        .ocrText(CERTIFICATE_NO + " 有效期 发证日期")
                        .build())),
                new ImageEvidenceMatcher());
        RagConfigDO.SearchParams searchParams = new RagConfigDO.SearchParams();
        searchParams.setResultCount(3);
        RagSearchContext context = RagSearchContext.builder()
                .query(CERTIFICATE_NO + "在哪里")
                .knowledge(RagPO.builder().id(100L).build())
                .searchParams(searchParams)
                .results(List.of())
                .build();

        handler.handle(context);

        assertEquals(1, context.getResults().size());
        SearchResult result = context.getResults().get(0);
        assertEquals(300L, result.getChunkId());
        assertEquals(200L, result.getDocumentId());
        assertTrue(result.getContent().contains(CERTIFICATE_NO));
    }

    @Test
    void handleDoesNotDuplicateExistingChunkResult() {
        ImageEvidenceRecallHandler handler = new ImageEvidenceRecallHandler(
                mapperReturning(List.of(RagDocumentImagePO.builder()
                        .id(1L)
                        .ragId(100L)
                        .chunkId(300L)
                        .ocrText(CERTIFICATE_NO)
                        .build())),
                new ImageEvidenceMatcher());
        RagConfigDO.SearchParams searchParams = new RagConfigDO.SearchParams();
        searchParams.setResultCount(3);
        RagSearchContext context = RagSearchContext.builder()
                .query(CERTIFICATE_NO)
                .knowledge(RagPO.builder().id(100L).build())
                .searchParams(searchParams)
                .results(List.of(SearchResult.builder().chunkId(300L).content("existing").build()))
                .build();

        handler.handle(context);

        assertEquals(1, context.getResults().size());
        assertEquals("existing", context.getResults().get(0).getContent());
    }

    private RagDocumentImageMapper mapperReturning(List<RagDocumentImagePO> images) {
        return (RagDocumentImageMapper) Proxy.newProxyInstance(
                RagDocumentImageMapper.class.getClassLoader(),
                new Class[]{RagDocumentImageMapper.class},
                (proxy, method, args) -> "selectList".equals(method.getName()) ? images : null);
    }
}
