package com.aizuda.snail.ai.model.rerank;

import com.aizuda.snail.ai.common.model.RerankApiClient;
import com.aizuda.snail.ai.model.common.ModelAdapterException;
import com.aizuda.snail.ai.model.common.ModelErrorCode;
import org.springframework.core.Ordered;

import java.util.Comparator;
import java.util.List;

public class RerankModelRuntime {

    private final List<RerankModelAdapter> adapters;

    public RerankModelRuntime(List<RerankModelAdapter> adapters) {
        this.adapters = adapters == null ? List.of() : adapters.stream()
                .sorted(Comparator.comparingInt(RerankModelRuntime::order))
                .toList();
    }

    public RerankApiClient build(RerankModelSpec spec) {
        validate(spec);
        RerankModelAdapter adapter = adapters.stream()
                .filter(candidate -> spec.adapterKey().equalsIgnoreCase(candidate.adapterKey()))
                .findFirst()
                .orElseThrow(() -> new ModelAdapterException(
                        ModelErrorCode.ADAPTER_NOT_FOUND,
                        "No RerankModelAdapter for adapterKey=" + spec.adapterKey()));
        try {
            return adapter.create(spec);
        } catch (ModelAdapterException e) {
            throw e;
        } catch (Exception e) {
            throw new ModelAdapterException(
                    ModelErrorCode.MODEL_BUILD_FAILED,
                    "RerankApiClient build failed: " + e.getMessage(), e);
        }
    }

    private static void validate(RerankModelSpec spec) {
        if (spec == null) {
            throw new ModelAdapterException(ModelErrorCode.INVALID_MODEL_CONFIG, "RerankModelSpec is required");
        }
        require(spec.adapterKey(), "adapterKey");
        require(spec.baseUrl(), "baseUrl");
        require(spec.apiKey(), "apiKey");
        require(spec.modelKey(), "modelKey");
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ModelAdapterException(ModelErrorCode.INVALID_MODEL_CONFIG, field + " is required");
        }
    }

    private static int order(RerankModelAdapter adapter) {
        return adapter instanceof Ordered ordered ? ordered.getOrder() : Ordered.LOWEST_PRECEDENCE;
    }
}
