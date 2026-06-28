package com.aizuda.snail.ai.search.storage.search.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchDeleteRequest {

    private String indexName;

    private List<String> ids;
}
