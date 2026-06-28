package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
public class StoreInstanceTestRequestVO {

    @NotBlank
    private String type;

    @NotBlank
    private Map<String, Object> config;
}
