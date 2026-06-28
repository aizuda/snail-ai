package com.aizuda.snail.ai.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量维度约束信息（模型上限 + 向量库上限 + 有效上限）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorDimensionConstraintVO {

    /**
     * 模型最大支持维度。
     */
    private Integer modelMaxDimension;

    /**
     * 向量库最大支持维度。
     */
    private Integer storeMaxDimension;

    /**
     * 实际有效最大维度（取两者最小值）。
     */
    private Integer effectiveMaxDimension;
}
