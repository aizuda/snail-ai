package com.aizuda.snail.ai.admin.vo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelAdapterOptionVO {

    private String adapterKey;

    private String name;

    private String modelType;

    private Boolean isDefault;
}
