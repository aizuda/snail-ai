package com.aizuda.snail.ai.vector.storage.vector.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VectorDocument {

    private String id;

    private String content;

    private float[] embedding;

    private Map<String, Object> metadata;
}
