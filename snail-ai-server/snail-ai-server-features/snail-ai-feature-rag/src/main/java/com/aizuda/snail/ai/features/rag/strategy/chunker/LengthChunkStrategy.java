package com.aizuda.snail.ai.features.rag.strategy.chunker;

import com.aizuda.snail.ai.features.rag.enums.ChunkModeEnum;
import org.springframework.stereotype.Component;

/**
 * 按长度：不做一级切分，整篇作为一个段落进入二级递归。
 */
@Component
public class LengthChunkStrategy extends AbstractChunkStrategy {

    public LengthChunkStrategy(TokenAwareChunker chunker) {
        super(chunker);
    }

    @Override
    public boolean supports(ChunkModeEnum mode) {
        return ChunkModeEnum.DEFAULT == mode;
    }

    @Override
    protected String[] splitIntoParagraphs(ChunkContext ctx) {
        return new String[] { ctx.getContent() };
    }
}
