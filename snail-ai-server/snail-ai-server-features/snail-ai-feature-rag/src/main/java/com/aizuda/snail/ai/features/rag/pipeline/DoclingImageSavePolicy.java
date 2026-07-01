package com.aizuda.snail.ai.features.rag.pipeline;

import com.aizuda.snail.ai.features.rag.docling.DoclingProperties;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;

/**
 * Docling 图片保存策略，统一合并全局配置与知识库级覆盖配置。
 *
 * @author opensnail
 * @since 2026-06-30
 */
final class DoclingImageSavePolicy {

    private final int maxImageCount;
    private final long maxImageBytes;

    private DoclingImageSavePolicy(int maxImageCount, long maxImageBytes) {
        this.maxImageCount = maxImageCount;
        this.maxImageBytes = maxImageBytes;
    }

    static DoclingImageSavePolicy from(DoclingProperties properties, RagConfigDO config) {
        int globalMaxImageCount = resolveGlobalMaxImageCount(properties);
        long globalMaxImageBytes = resolveGlobalMaxImageBytes(properties);
        RagConfigDO.DoclingParams doclingParams = resolveDoclingParams(config);

        int effectiveMaxImageCount = doclingParams == null
                ? globalMaxImageCount
                : positiveOrDefault(doclingParams.getMaxImageCount(), globalMaxImageCount);
        long effectiveMaxImageBytes = doclingParams == null
                ? globalMaxImageBytes
                : positiveOrDefault(doclingParams.getMaxImageBytes(), globalMaxImageBytes);
        return new DoclingImageSavePolicy(effectiveMaxImageCount, effectiveMaxImageBytes);
    }

    boolean isCountExceeded(int savedCount) {
        return savedCount >= maxImageCount;
    }

    boolean isOversized(long imageBytes) {
        return imageBytes > maxImageBytes;
    }

    int getMaxImageCount() {
        return maxImageCount;
    }

    long getMaxImageBytes() {
        return maxImageBytes;
    }

    private static RagConfigDO.DoclingParams resolveDoclingParams(RagConfigDO config) {
        if (config == null || config.getParseParams() == null) {
            return null;
        }
        return config.getParseParams().getDocling();
    }

    private static int resolveGlobalMaxImageCount(DoclingProperties properties) {
        if (properties == null) {
            return DoclingProperties.DEFAULT_MAX_IMAGE_COUNT;
        }
        return positiveOrDefault(properties.getMaxImageCount(), DoclingProperties.DEFAULT_MAX_IMAGE_COUNT);
    }

    private static long resolveGlobalMaxImageBytes(DoclingProperties properties) {
        if (properties == null) {
            return DoclingProperties.DEFAULT_MAX_IMAGE_BYTES;
        }
        return positiveOrDefault(properties.getMaxImageBytes(), DoclingProperties.DEFAULT_MAX_IMAGE_BYTES);
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private static long positiveOrDefault(Long value, long defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }
}
