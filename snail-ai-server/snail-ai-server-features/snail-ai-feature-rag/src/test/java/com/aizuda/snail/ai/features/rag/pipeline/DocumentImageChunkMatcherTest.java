package com.aizuda.snail.ai.features.rag.pipeline;

import com.aizuda.snail.ai.features.rag.docling.DoclingParseResult;
import com.aizuda.snail.ai.persistence.rag.po.RagChunkPO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DocumentImageChunkMatcherTest {

    private final DocumentImageChunkMatcher matcher = new DocumentImageChunkMatcher();

    @Test
    void matchReturnsChunkIdWhenChunkContainsCaptionAndOcrText() {
        DoclingParseResult.DoclingImage image = DoclingParseResult.DoclingImage.builder()
                .caption("图 5 合格证列表")
                .ocrText("合格证编号 状态 有效期")
                .build();
        List<RagChunkPO> chunks = List.of(
                RagChunkPO.builder()
                        .id(10L)
                        .content("2.3 证照管理\n普通正文")
                        .build(),
                RagChunkPO.builder()
                        .id(11L)
                        .content("[图片说明: 图 5 合格证列表，合格证编号 状态 有效期]")
                        .build()
        );

        Long chunkId = matcher.matchChunkId(image, chunks);

        assertEquals(11L, chunkId);
    }

    @Test
    void matchReturnsNullWhenImageHasNoSearchableText() {
        DoclingParseResult.DoclingImage image = DoclingParseResult.DoclingImage.builder()
                .caption("")
                .ocrText("")
                .build();

        Long chunkId = matcher.matchChunkId(image, List.of(RagChunkPO.builder().id(10L).content("body").build()));

        assertNull(chunkId);
    }

    @Test
    void matchReturnsNullWhenCaptionOnlyHasFigureNumber() {
        DoclingParseResult.DoclingImage image = DoclingParseResult.DoclingImage.builder()
                .caption("图 1")
                .build();
        List<RagChunkPO> chunks = List.of(RagChunkPO.builder()
                .id(10L)
                .content("[图片说明: 图 1]")
                .build());

        Long chunkId = matcher.matchChunkId(image, chunks);

        assertNull(chunkId);
    }

    @Test
    void matchReturnsChunkIdWhenImageChunkContainsFigureTitle() {
        DoclingParseResult.DoclingImage image = DoclingParseResult.DoclingImage.builder()
                .figureTitle("系统架构图")
                .sectionTitle("总体设计")
                .build();
        List<RagChunkPO> chunks = List.of(RagChunkPO.builder()
                .id(12L)
                .content("图片知识\n图标题：系统架构图\n章节：总体设计")
                .build());

        Long chunkId = matcher.matchChunkId(image, chunks);

        assertEquals(12L, chunkId);
    }
}
