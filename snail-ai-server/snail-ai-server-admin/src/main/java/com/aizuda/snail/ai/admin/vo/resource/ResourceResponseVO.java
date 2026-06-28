package com.aizuda.snail.ai.admin.vo.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResourceResponseVO {
    private Long id;
    private String storageKey;
    private String originalName;
    private Long fileSize;
    private String mimeType;
    private String storageType;
    private String accessUrl;
    private String bizType;
    private Long bizId;
    private Long creatorId;
    private LocalDateTime createDt;
}
