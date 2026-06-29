package com.aizuda.snail.ai.features.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档切片数据，表示文本分块后的一段内容及其段落/分块位置与 token 统计。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChunkDTO {
    /**
     * 所属段落序号
     */
    private int paragraphIndex;
    /**
     * 分块序号（切片在文档中的顺序）
     */
    private int chunkIndex;
    /**
     * 切片文本内容
     */
    private String content;
    /**
     * 切片的 token 数量
     */
    private int tokenCount;
}
