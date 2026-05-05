package com.aizuda.snail.ai.features.rag.strategy.importer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImportRequest {

    private Long ragId;

    private MultipartFile file;

    private String url;

    private String textContent;

    private String name;

    private String sourceType;
}
