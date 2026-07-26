package com.aizuda.snail.ai.features.rag.pipeline;

import com.aizuda.snail.ai.features.rag.dto.ChunkDTO;
import com.aizuda.snail.ai.features.rag.enums.ChunkModeEnum;
import com.aizuda.snail.ai.features.rag.strategy.chunker.ChunkContext;
import com.aizuda.snail.ai.features.rag.strategy.chunker.ChunkStrategy;
import com.aizuda.snail.ai.features.rag.strategy.chunker.ChunkStrategyFactory;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentChunkingServiceTest {

    @Test
    void chunkFallsBackToDefaultsWhenKnowledgeConfigIsMalformed() {
        CapturingChunkStrategy strategy = new CapturingChunkStrategy();
        DocumentChunkingService service = new DocumentChunkingService(new ChunkStrategyFactory(List.of(strategy)));
        RagPO knowledge = RagPO.builder()
                .id(1L)
                .delimiter("\n")
                .config("{")
                .build();

        List<ChunkDTO> chunks = service.chunk("hello", knowledge);

        assertEquals(1, chunks.size());
        assertEquals(2000, strategy.context.getMaxTokens());
        assertEquals(0, strategy.context.getOverlap());
        assertEquals("\n", strategy.context.getCustomDelimiter());
    }

    private static class CapturingChunkStrategy implements ChunkStrategy {
        private ChunkContext context;

        @Override
        public boolean supports(ChunkModeEnum mode) {
            return mode == ChunkModeEnum.DEFAULT;
        }

        @Override
        public List<ChunkDTO> chunk(ChunkContext context) {
            this.context = context;
            return List.of(ChunkDTO.builder()
                    .content(context.getContent())
                    .tokenCount(context.getContent().length())
                    .build());
        }
    }
}
