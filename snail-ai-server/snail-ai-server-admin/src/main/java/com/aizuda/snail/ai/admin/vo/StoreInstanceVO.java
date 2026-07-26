package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 存储实例视图对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class StoreInstanceVO {

    /** 实例 ID */
    private Long id;

    /** 实例名称 */
    @NotBlank
    private String name;

    /** 实例分类 */
    @NotNull
    private Integer category;

    /** 实例类型 */
    @NotNull
    private Integer type;

    /** 配置信息 */
    private Map<String, Object> config;

    /** 状态 */
    private Integer status;

    /** 是否默认实例 */
    private Boolean isDefault;

    /** 创建时间 */
    private LocalDateTime createDt;

    /** 更新时间 */
    private LocalDateTime updateDt;
}
