package com.aizuda.snail.ai.model.common;

public record ModelAdapterDescriptor(
        String adapterKey,
        String name,
        ModelCapability capability,
        boolean enabled,
        int order) {

    public ModelAdapterDescriptor {
        if (adapterKey == null || adapterKey.isBlank()) {
            throw new IllegalArgumentException("adapterKey is required");
        }
        if (capability == null) {
            throw new IllegalArgumentException("capability is required");
        }
    }

    public static ModelAdapterDescriptor of(String adapterKey, String name, ModelCapability capability) {
        return new ModelAdapterDescriptor(adapterKey, name, capability, true, 0);
    }
}
