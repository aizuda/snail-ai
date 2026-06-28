package com.aizuda.snail.ai.features.rag.strategy.chunker;

import com.aizuda.snail.ai.features.rag.enums.ChunkModeEnum;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按分隔符一级切分，再递归按长度切分。
 */
@Component
public class DelimiterChunkStrategy extends AbstractChunkStrategy {

    public DelimiterChunkStrategy(TokenAwareChunker chunker) {
        super(chunker);
    }

    @Override
    public boolean supports(ChunkModeEnum mode) {
        return ChunkModeEnum.DELIMITER == mode;
    }

    @Override
    protected String[] splitIntoParagraphs(ChunkContext ctx) {
        List<String> delimiters = chunker.resolveDelimiterList(ctx.getCustomDelimiter());
        return chunker.splitByAnyDelimiter(ctx.getContent(), delimiters);
    }
}
