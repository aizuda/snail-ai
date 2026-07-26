package com.aizuda.snail.ai.features.rag.search.pipeline.handler;

import com.aizuda.snail.ai.common.dto.rag.SearchResult;
import com.aizuda.snail.ai.features.rag.enums.RagChunkSourceTypeEnum;
import com.aizuda.snail.ai.features.rag.search.pipeline.RagSearchContext;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.persistence.rag.mapper.RagChunkMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentImageMapper;
import com.aizuda.snail.ai.persistence.rag.mapper.RagDocumentMapper;
import com.aizuda.snail.ai.persistence.rag.po.RagChunkPO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinalizeHandlerTest {

    @Test
    void handleDoesNotExpandNearbySlicesForImageChunk() {
        RagChunkPO imageChunk = RagChunkPO.builder()
                .id(20L)
                .documentId(2L)
                .chunkIndex(10)
                .sourceType(RagChunkSourceTypeEnum.IMAGE.getValue())
                .content("图片知识\n图标题：系统架构图")
                .build();
        RagChunkPO nearbyTextChunk = RagChunkPO.builder()
                .id(21L)
                .documentId(2L)
                .chunkIndex(11)
                .sourceType(RagChunkSourceTypeEnum.TEXT.getValue())
                .content("普通正文不应该拼到图片知识结果里")
                .build();
        FinalizeHandler handler = new FinalizeHandler(
                chunkMapperReturning(imageChunk, List.of(imageChunk, nearbyTextChunk)),
                documentMapperReturningEmpty(),
                new SearchResultImageEnricher(imageMapperReturningEmpty()));
        RagConfigDO.ModelParams modelParams = new RagConfigDO.ModelParams();
        modelParams.setNearbySliceCount(1);
        RagConfigDO.SearchParams searchParams = new RagConfigDO.SearchParams();
        searchParams.setResultCount(1);
        RagSearchContext context = RagSearchContext.builder()
                .query("系统架构图")
                .searchParams(searchParams)
                .modelParams(modelParams)
                .results(List.of(SearchResult.builder().chunkId(20L).build()))
                .build();

        handler.handle(context);

        assertEquals("图片知识\n图标题：系统架构图", context.getResults().get(0).getContent());
    }

    private RagChunkMapper chunkMapperReturning(RagChunkPO chunk, List<RagChunkPO> nearbyChunks) {
        return (RagChunkMapper) Proxy.newProxyInstance(
                RagChunkMapper.class.getClassLoader(),
                new Class[]{RagChunkMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectById" -> chunk;
                    case "selectByIds" -> List.of(chunk);
                    case "selectList" -> nearbyChunks;
                    default -> null;
                });
    }

    private RagDocumentMapper documentMapperReturningEmpty() {
        return (RagDocumentMapper) Proxy.newProxyInstance(
                RagDocumentMapper.class.getClassLoader(),
                new Class[]{RagDocumentMapper.class},
                (proxy, method, args) -> "selectByIds".equals(method.getName()) ? List.of() : null);
    }

    private RagDocumentImageMapper imageMapperReturningEmpty() {
        return (RagDocumentImageMapper) Proxy.newProxyInstance(
                RagDocumentImageMapper.class.getClassLoader(),
                new Class[]{RagDocumentImageMapper.class},
                (proxy, method, args) -> "selectList".equals(method.getName()) ? List.of() : null);
    }
}
