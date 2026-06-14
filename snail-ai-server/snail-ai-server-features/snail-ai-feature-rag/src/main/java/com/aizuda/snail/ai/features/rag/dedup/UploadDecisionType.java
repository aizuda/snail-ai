package com.aizuda.snail.ai.features.rag.dedup;

/**
 * 上传决策类型
 *
 * @author opensnail
 */
public enum UploadDecisionType {

    /** 新文件，正常入库 */
    NEW,

    /** 命中重复且策略为拒绝 */
    REJECT,

    /** 命中重复且策略为跳过 */
    SKIP,

    /** 命中重复且策略为覆盖 */
    OVERWRITE
}
