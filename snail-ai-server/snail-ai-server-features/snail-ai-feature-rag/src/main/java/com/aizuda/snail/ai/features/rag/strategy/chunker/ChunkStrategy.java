package com.aizuda.snail.ai.features.rag.strategy.chunker;

import com.aizuda.snail.ai.features.rag.dto.ChunkDTO;
import com.aizuda.snail.ai.features.rag.enums.ChunkModeEnum;

import java.util.List;

/**
 * 文档切片策略：按切片模式将文档全文切分为带重叠的片段列表。
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
