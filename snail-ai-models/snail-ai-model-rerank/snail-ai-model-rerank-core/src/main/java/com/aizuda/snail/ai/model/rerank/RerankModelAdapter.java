package com.aizuda.snail.ai.model.rerank;

import com.aizuda.snail.ai.common.model.RerankApiClient;

/**
 * 重排模型适配器接口，按规格创建对应供应商的 {@link RerankApiClient}。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface RerankModelAdapter {

    /**
     * 适配器唯一标识，用于与模型配置的 adapterKey 匹配
     *
     * @return 适配器键
     */
    String adapterKey();

    /**
     * 按规格创建重排客户端
     *
     * @param spec 重排模型构建规格
     * @return 重排客户端实例
     */
    RerankApiClient create(RerankModelSpec spec);
}
