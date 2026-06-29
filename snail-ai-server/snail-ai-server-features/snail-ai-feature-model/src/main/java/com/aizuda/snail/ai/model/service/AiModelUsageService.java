package com.aizuda.snail.ai.model.service;

/**
 * AI模型使用统计服务接口（基础设施层）
 * 仅包含模型基础设施所需的方法，由 admin 层实现
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface AiModelUsageService {

    /**
     * 记录一次模型调用的用量统计
     *
     * @param modelId          模型配置 ID
     * @param userId           用户 ID
     * @param promptTokens     输入 token 数
     * @param completionTokens 输出 token 数
     * @param responseTime     响应耗时（毫秒）
     * @param status           调用状态（成功 / 失败）
     * @param errorMessage     失败时的错误信息
     * @param conversationId   会话 ID
     */
    void recordUsage(Long modelId, Long userId, Integer promptTokens, Integer completionTokens,
                     Long responseTime, Integer status, String errorMessage, String conversationId);
}
