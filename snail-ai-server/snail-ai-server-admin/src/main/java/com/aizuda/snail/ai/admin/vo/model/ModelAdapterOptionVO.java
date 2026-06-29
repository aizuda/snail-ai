package com.aizuda.snail.ai.admin.vo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型适配器下拉选项
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelAdapterOptionVO {

    /** 适配器标识 */
    private String adapterKey;

    /** 适配器名称 */
    private String name;

    /** 模型类型 */
    private String modelType;

    /** 是否为默认适配器 */
    private Boolean isDefault;
}
