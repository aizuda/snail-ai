package com.aizuda.snail.ai.features.rag.pipeline;

import com.aizuda.snail.ai.features.rag.docling.DoclingParseResult;
import com.aizuda.snail.ai.features.rag.enums.RagChunkSourceTypeEnum;
import com.aizuda.snail.ai.features.rag.strategy.chunker.TokenAwareChunker;
import com.aizuda.snail.ai.persistence.rag.po.RagChunkPO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentImageKnowledgeChunkBuilderTest {

    private final DocumentImageKnowledgeChunkBuilder builder = new DocumentImageKnowledgeChunkBuilder(
            new ImageOcrQualityEvaluator(),
            new TokenAwareChunker());

    @Test
    void buildCreatesSearchableImageChunkFromCaptionTitleAndReadableOcr() {
        DoclingParseResult.DoclingImage image = DoclingParseResult.DoclingImage.builder()
                .page(3)
                .figureNo("图 2")
                .figureTitle("系统架构图")
                .sectionTitle("总体设计")
                .caption("服务层调用向量库和大模型")
                .ocrText("用户问题经过检索增强后进入大模型生成答案")
                .build();
        ImageKnowledgeChunkBuildRequest request = ImageKnowledgeChunkBuildRequest.builder()
                .ragId(1L)
                .documentId(2L)
                .documentName("交付方案.pdf")
                .startChunkIndex(5)
                .images(List.of(image))
                .build();

        List<RagChunkPO> chunks = builder.build(request);

        assertEquals(1, chunks.size());
        RagChunkPO chunk = chunks.get(0);
        assertEquals(RagChunkSourceTypeEnum.IMAGE.getValue(), chunk.getSourceType());
        assertEquals(5, chunk.getChunkIndex());
        assertTrue(chunk.getContent().contains("系统架构图"));
        assertTrue(chunk.getContent().contains("服务层调用向量库和大模型"));
        assertTrue(chunk.getContent().contains("用户问题经过检索增强后进入大模型生成答案"));
    }

    @Test
    void buildKeepsNoisyOcrOutOfImageChunkContent() {
        DoclingParseResult.DoclingImage image = DoclingParseResult.DoclingImage.builder()
                .page(1)
                .figureTitle("登录页面")
                .ocrText("@@@\n1\n2\n-\n///")
                .build();
        ImageKnowledgeChunkBuildRequest request = ImageKnowledgeChunkBuildRequest.builder()
                .ragId(1L)
                .documentId(2L)
                .documentName("操作手册.pdf")
                .startChunkIndex(0)
                .images(List.of(image))
                .build();

        List<RagChunkPO> chunks = builder.build(request);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).getContent().contains("登录页面"));
        assertFalse(chunks.get(0).getContent().contains("@@@"));
    }

    @Test
    void buildSkipsImageWithoutSearchableKnowledge() {
        DoclingParseResult.DoclingImage image = DoclingParseResult.DoclingImage.builder()
                .page(1)
                .figureNo("图 1")
                .ocrText("@@@\n1")
                .build();
        ImageKnowledgeChunkBuildRequest request = ImageKnowledgeChunkBuildRequest.builder()
                .ragId(1L)
                .documentId(2L)
                .documentName("操作手册.pdf")
                .startChunkIndex(0)
                .images(List.of(image))
                .build();

        assertTrue(builder.build(request).isEmpty());
    }
}
