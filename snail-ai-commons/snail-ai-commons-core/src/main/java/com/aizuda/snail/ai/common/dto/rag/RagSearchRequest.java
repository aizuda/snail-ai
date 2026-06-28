package com.aizuda.snail.ai.common.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RagSearchRequest
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchRequest {

    private Long ragId;

    private String query;
}
