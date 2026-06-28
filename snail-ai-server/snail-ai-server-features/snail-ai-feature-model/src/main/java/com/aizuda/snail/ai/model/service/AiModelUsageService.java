package com.aizuda.snail.ai.model.service;

/**
 * AI模型使用统计服务接口（基础设施层）
 * 仅包含模型基础设施所需的方法，由 admin 层实现
 */
public interface AiModelUsageService {

    void recordUsage(Long modelId, Long userId, Integer promptTokens, Integer completionTokens,
                     Long responseTime, Integer status, String errorMessage, String conversationId);
}
