package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 存储实例连接测试请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class StoreInstanceTestRequestVO {

    /** 实例分类；旧客户端可不传，由类型兼容推断 */
    private Integer category;

    /** 实例类型 */
    @NotBlank
    private String type;

    /** 配置信息 */
    @NotNull
    private Map<String, Object> config;
}
