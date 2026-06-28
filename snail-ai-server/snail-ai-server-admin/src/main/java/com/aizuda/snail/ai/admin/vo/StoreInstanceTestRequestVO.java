package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * StoreInstanceTestRequestVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class StoreInstanceTestRequestVO {

    @NotBlank
    private String type;

    @NotBlank
    private Map<String, Object> config;
}
