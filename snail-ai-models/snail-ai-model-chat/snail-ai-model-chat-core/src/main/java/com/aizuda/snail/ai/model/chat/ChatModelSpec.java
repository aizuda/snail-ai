package com.aizuda.snail.ai.model.chat;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;

/**
 * 对话模型构建规格，封装创建对话模型所需的适配器、供应商、连接与模型参数。
 *
 * @param adapterKey  适配器键
 * @param providerKey 供应商键
 * @param baseUrl     模型服务基础地址
 * @param apiKey      访问密钥
 * @param modelKey    模型标识
 * @param configJson  扩展配置（温度、最大 token 等）
 * @author opensnail
 * @since 2026-06-14
 */
public record ChatModelSpec(
        String adapterKey,
        String providerKey,
        String baseUrl,
        String apiKey,
        String modelKey,
        ConfigExtAttrsDTO configJson) {
}
