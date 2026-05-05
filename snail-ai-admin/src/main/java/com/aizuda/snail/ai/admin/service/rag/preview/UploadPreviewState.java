package com.aizuda.snail.ai.admin.service.rag.preview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预览状态：存于 Redis，commit 时解析回放
 *
 * @author opensnail
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadPreviewState {

    private Long ragId;
    private Long userId;
    private Integer dedupStrategy;
    private Integer dedupAction;
    private List<UploadPreviewItemState> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UploadPreviewItemState {
        private Long tempResourceId;
        private String fileName;
        private String fileType;
        private String sourceType;
        private Long fileSize;
        private String contentHash;
        /** 预览阶段的初始决策 */
        private String decision;
        /** 命中维度 */
        private String matchType;
        /** 冲突的旧文档 ID */
        private Long conflictDocumentId;
    }
}
