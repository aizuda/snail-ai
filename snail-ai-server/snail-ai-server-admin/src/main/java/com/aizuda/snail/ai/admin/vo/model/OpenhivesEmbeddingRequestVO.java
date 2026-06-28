package com.aizuda.snail.ai.admin.vo.model;

import lombok.Data;

/**
 * 按问题 ID 触发向量化的请求。
 *
 * @author opensnail
 * @date 2025-07-12
 */
@Data
public class OpenhivesEmbeddingRequestVO {
    private Long questionId;
}
