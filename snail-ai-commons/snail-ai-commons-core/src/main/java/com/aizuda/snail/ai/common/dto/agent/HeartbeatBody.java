package com.aizuda.snail.ai.common.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 心跳上报数据
 *
 * @author opensnail
 * @date 2025-04-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeartbeatBody {

    private int maxConcurrentChats;
    private int activeChats;
}
