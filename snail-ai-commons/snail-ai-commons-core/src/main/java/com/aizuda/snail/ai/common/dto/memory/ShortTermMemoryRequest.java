package com.aizuda.snail.ai.common.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加载短期对话历史请求
 *
 * @author opensnail
 * @date 2025-04-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortTermMemoryRequest {

    private String conversationId;
    private Integer windowSize;
}
