package com.aizuda.snail.ai.model.rerank;

import com.aizuda.snail.ai.common.model.RerankApiClient;

/**
 * RerankModelAdapter
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface RerankModelAdapter {

    String adapterKey();

    RerankApiClient create(RerankModelSpec spec);
}
