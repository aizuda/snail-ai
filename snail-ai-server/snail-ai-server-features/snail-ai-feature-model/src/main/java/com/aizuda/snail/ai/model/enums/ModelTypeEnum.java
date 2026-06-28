package com.aizuda.snail.ai.model.enums;

/**
 * 模型类型枚举
 * 支持多种AI模型的分类
 */
public enum ModelTypeEnum {
    /**
     * 对话模型 - 用于对话、问答、文本生成
     */
    CHAT("CHAT", "对话模型"),

    /**
     * 向量模型 - 用于文本向量化、相似度计算
     */
    EMBEDDING("EMBEDDING", "向量模型"),

    /**
     * 重排模型 - 用于搜索结果重排
     */
    RERANKER("RERANKER", "重排模型"),

    /**
     * 图像模型 - 用于图像生成、理解
     */
    IMAGE("IMAGE", "图像模型"),

    /**
     * 语音模型 - 用于语音识别、生成
     */
    SPEECH("SPEECH", "语音模型");

    private final String value;
    private final String name;

    ModelTypeEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static ModelTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ModelTypeEnum type : ModelTypeEnum.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
