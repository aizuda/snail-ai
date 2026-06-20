package com.aizuda.snail.ai.admin.vo.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RagDocumentResponseVO {

    private Long id;

    private Long ragId;

    private String name;

    private String fileType;

    private String sourceType;

    private Integer status;

    private String errorMsg;

    private Integer chunkCount;

    private Long fileSize;

    private Long resourceId;

    private String previewUrl;

    private String downloadUrl;

    /**
     * 决策结果: NEW / SKIP / OVERWRITE
     * REJECT 通过异常返回，不会进入此字段
     */
    private String decision;

    /** 命中的冲突维度: NONE / BY_NAME / BY_CONTENT / BOTH */
    private String matchType;

    /** 冲突的旧文档 ID，仅在 SKIP/OVERWRITE 时返回 */
    private Long conflictDocumentId;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
