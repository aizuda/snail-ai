package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Data;

@Data
public class StoreInstanceTestRequestVO {

    @NotBlank private String type;

    @NotBlank private Map<String, Object> config;
}
