package com.aizuda.snail.ai.model.common;

/**
 * 模型适配器描述符，声明适配器的标识、名称、能力、启用状态与排序。
 *
 * @param adapterKey 适配器唯一标识
 * @param name       适配器展示名称
 * @param capability 适配器支持的模型能力
 * @param enabled    是否启用
 * @param order      排序值（越小优先级越高）
 * @author opensnail
 * @since 2026-06-14
 */
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
