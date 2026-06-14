package com.aizuda.snail.ai.admin.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class StoreInstanceVO {

    private Long id;

    @NotBlank
    private String name;

    @NotNull
    private Integer category;

    @NotNull
    private Integer type;

    private Map<String, Object> config;

    private Integer status;

    private Boolean isDefault;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
