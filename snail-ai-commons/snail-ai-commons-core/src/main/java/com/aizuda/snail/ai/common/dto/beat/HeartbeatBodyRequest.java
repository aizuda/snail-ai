package com.aizuda.snail.ai.common.dto.beat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 心跳上报数据
 * <p>
 * author: zhangshuguang
 * date: 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeartbeatBodyRequest {
    private int maxConcurrentChats;
    private int activeChats;
}
