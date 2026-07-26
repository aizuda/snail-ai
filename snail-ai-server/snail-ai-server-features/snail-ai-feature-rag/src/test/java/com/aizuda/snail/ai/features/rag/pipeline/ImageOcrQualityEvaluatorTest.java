package com.aizuda.snail.ai.features.rag.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageOcrQualityEvaluatorTest {

    @Test
    void isIndexableRejectsFragmentedNoise() {
        assertFalse(new ImageOcrQualityEvaluator().isIndexable("@@@\n1\n2\n-\n///"));
    }

    @Test
    void isIndexableAcceptsReadableChineseText() {
        assertTrue(new ImageOcrQualityEvaluator().isIndexable("系统整体架构包含服务层、向量库和大模型调用链路。"));
    }
}
