package com.aizuda.snail.ai.model.rerank;

import com.aizuda.snail.ai.common.model.RerankApiClient;

public interface RerankModelAdapter {

    String adapterKey();

    RerankApiClient create(RerankModelSpec spec);
}
