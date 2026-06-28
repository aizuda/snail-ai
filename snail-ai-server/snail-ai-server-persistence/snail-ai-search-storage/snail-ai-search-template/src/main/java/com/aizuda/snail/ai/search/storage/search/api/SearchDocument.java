package com.aizuda.snail.ai.search.storage.search.api;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchDocument {

    private String id;

    private String content;

    private Map<String, Object> metadata;
}
