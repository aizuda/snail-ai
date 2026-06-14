package com.aizuda.snail.ai.features.rag.dedup;

/**
 * 去重命中维度：用于向用户/前端解释为什么这次上传被识别为重复
 *
 * @author opensnail
 */
public enum DedupMatchType {

    /** 未命中 */
    NONE,

    /** 文件名命中 */
    BY_NAME,

    /** 文件内容命中 */
    BY_CONTENT,

    /** 文件名与内容均命中 */
    BOTH
}
