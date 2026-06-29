package com.aizuda.snail.ai.common.model.embedding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 用量统计，记录提示词与总 Token 消耗。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Usage {

    /**
     * 提示词 Token 数
     */
    private Integer promptTokens;

    /**
     * 总 Token 数
     */
    private Integer totalTokens;
}
