package com.aizuda.snail.ai.features.rag.pipeline;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.features.rag.dto.ChunkDTO;
import com.aizuda.snail.ai.features.rag.enums.ChunkModeEnum;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import com.aizuda.snail.ai.persistence.rag.po.RagPO;
import com.aizuda.snail.ai.features.rag.strategy.chunker.ChunkContext;
import com.aizuda.snail.ai.features.rag.strategy.chunker.ChunkStrategy;
import com.aizuda.snail.ai.features.rag.strategy.chunker.ChunkStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 按知识库 config.chunkParams.mode 选择切片策略。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentChunkingService {
    private final ChunkStrategyFactory chunkStrategyFactory;

    public List<ChunkDTO> chunk(String content, RagPO knowledge) {
        RagConfigDO cfg = parseKnowledgeConfig(knowledge);
        RagConfigDO.ChunkParams cp = cfg != null ? cfg.getChunkParams() : null;
        String modeStr = cp != null ? cp.getMode() : null;
        ChunkModeEnum mode = ChunkModeEnum.fromValue(modeStr);

        ChunkContext context = ChunkContext.builder()
                .content(content)
                .maxTokens(resolveChunkMaxTokens(cp))
                .overlap(resolveChunkOverlap(cp))
                .customDelimiter(resolveDelimiterForChunk(knowledge, cp))
                .chunkRegex( cp.getChunkRegex())
                .chunkModelId(cp.getChunkModelId())
                .build();

        ChunkStrategy strategy = chunkStrategyFactory.getStrategy(mode);
        return strategy.chunk(context);
    }

    private RagConfigDO parseKnowledgeConfig(RagPO k) {
        if (StrUtil.isBlank(k.getConfig())) {
            return null;
        }
        return JsonUtil.parseObject(k.getConfig(), RagConfigDO.class);
    }

    private String resolveDelimiterForChunk(RagPO k, RagConfigDO.ChunkParams cp) {
        if (cp != null && StrUtil.isNotBlank(cp.getCustomDelimiter())) {
            return cp.getCustomDelimiter().trim();
        }
        return k.getDelimiter();
    }

    private int resolveChunkMaxTokens(RagConfigDO.ChunkParams cp) {
        return Math.max(50, cp.getMaxChunkTokens() != null ? cp.getMaxChunkTokens() : 500);
    }

    private int resolveChunkOverlap(RagConfigDO.ChunkParams cp) {
        return Math.max(0, cp.getChunkOverlap() != null ? cp.getChunkOverlap() : 0);
    }
}
