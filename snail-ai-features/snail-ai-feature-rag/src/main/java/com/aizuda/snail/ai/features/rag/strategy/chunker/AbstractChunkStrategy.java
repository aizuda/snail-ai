package com.aizuda.snail.ai.features.rag.strategy.chunker;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.features.rag.dto.ChunkDTO;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 模板方法：子类实现一级切分 {@link #splitIntoParagraphs}，二级递归切分由 {@link TokenAwareChunker#chunkParagraphs} 统一处理。
 */
@RequiredArgsConstructor
public abstract class AbstractChunkStrategy implements ChunkStrategy {

    protected final TokenAwareChunker chunker;

    @Override
    public List<ChunkDTO> chunk(ChunkContext context) {
        if (StrUtil.isBlank(context.getContent())) {
            return List.of();
        }
        String[] paragraphs = splitIntoParagraphs(context);
        return chunker.chunkParagraphs(paragraphs, context.getMaxTokens(), context.getOverlap());
    }

    /**
     * 子类实现一级切分逻辑，将文档内容拆分为段落数组。
     * 二级递归切分（按 maxTokens）由抽象类统一调用。
     */
    protected abstract String[] splitIntoParagraphs(ChunkContext context);
}
