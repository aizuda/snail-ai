package com.aizuda.snail.ai.common.grpc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务端时间戳响应（通用响应对象）
 *
 * @author opensnail
 * @date 2025-04-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerTimestampResponse {
    
    private Long serverTimestamp;
}
