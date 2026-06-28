package com.aizuda.snail.ai.common.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索记忆上下文请求
 *
 * @author opensnail
 * @date 2025-04-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryRetrieveRequest {
    
    /**
     * Agent ID
     */
    private Long agentId;
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 对话 ID
     */
    private String conversationId;
    
    /**
     * 问题
     */
    private String query;
    
    /**
     * 记忆配置 ID
     */
    private Long memoryConfigId;
    
    /**
     * 模型 ID
     */
    private Long modelId;
    
    /**
     * Top K
     */
    private Integer topK;
}
