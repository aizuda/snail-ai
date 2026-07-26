package com.aizuda.snail.ai.model.rerank;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;

/**
 * 重排模型构建规格，封装创建重排客户端所需的适配器、供应商、连接与模型参数。
 *
 * @param adapterKey  适配器键
 * @param providerKey 供应商键
 * @param baseUrl     模型服务基础地址
 * @param apiKey      访问密钥
 * @param modelKey    模型标识
 * @param configJson  扩展配置（rerank 路径、超时等）
 * @author opensnail
 * @since 2026-06-14
 */
public record RerankModelSpec(
        String adapterKey,
        String providerKey,
        String baseUrl,
        String apiKey,
        String modelKey,
        ConfigExtAttrsDTO configJson) {
}
