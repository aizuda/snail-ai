package com.aizuda.snail.ai.search.storage.search.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchDeleteRequest {

    private String indexName;

    private List<String> ids;
}
