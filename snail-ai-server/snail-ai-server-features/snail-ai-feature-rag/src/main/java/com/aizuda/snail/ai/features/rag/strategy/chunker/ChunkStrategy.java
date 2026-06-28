package com.aizuda.snail.ai.features.rag.strategy.chunker;

import com.aizuda.snail.ai.features.rag.dto.ChunkDTO;
import com.aizuda.snail.ai.features.rag.enums.ChunkModeEnum;

import java.util.List;

/**
 * ChunkStrategy
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface ChunkStrategy {

    /** 是否支持该切片模式 */
    boolean supports(ChunkModeEnum mode);

    /** 执行切片 */
    List<ChunkDTO> chunk(ChunkContext context);
}
