package com.aizuda.snail.ai.model.common;

import java.util.List;

public final class ModelAdapterDefaults {

    public static final String CHAT_MODEL_TYPE = "CHAT";
    public static final String EMBEDDING_MODEL_TYPE = "EMBEDDING";
    public static final String RERANKER_MODEL_TYPE = "RERANKER";

    public static final String OPENAI_COMPATIBLE_ADAPTER = "openai-compatible";
    public static final String HTTP_ADAPTER = "http";

    private static final ModelAdapterDescriptor CHAT_OPENAI_COMPATIBLE = ModelAdapterDescriptor.of(
            OPENAI_COMPATIBLE_ADAPTER, "OpenAI Compatible", ModelCapability.CHAT);
    private static final ModelAdapterDescriptor EMBEDDING_OPENAI_COMPATIBLE = ModelAdapterDescriptor.of(
            OPENAI_COMPATIBLE_ADAPTER, "OpenAI Compatible", ModelCapability.EMBEDDING);
    private static final ModelAdapterDescriptor RERANK_HTTP = ModelAdapterDescriptor.of(
            HTTP_ADAPTER, "HTTP", ModelCapability.RERANKER);

    private ModelAdapterDefaults() {
    }

    public static String resolve(String adapterKey, String modelType) {
        if (adapterKey != null && !adapterKey.isBlank()) {
            return adapterKey.trim();
        }
        if (CHAT_MODEL_TYPE.equalsIgnoreCase(modelType) || EMBEDDING_MODEL_TYPE.equalsIgnoreCase(modelType)) {
            return OPENAI_COMPATIBLE_ADAPTER;
        }
        if (RERANKER_MODEL_TYPE.equalsIgnoreCase(modelType)) {
            return HTTP_ADAPTER;
        }
        return OPENAI_COMPATIBLE_ADAPTER;
    }

    public static List<ModelAdapterDescriptor> defaultDescriptors() {
        return List.of(CHAT_OPENAI_COMPATIBLE, EMBEDDING_OPENAI_COMPATIBLE, RERANK_HTTP);
    }
}
