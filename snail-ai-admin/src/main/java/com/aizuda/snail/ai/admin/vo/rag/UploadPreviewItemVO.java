package com.aizuda.snail.ai.admin.vo.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传预览：单文件预测结果
 *
 * @author opensnail
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadPreviewItemVO {

    /** 临时资源 ID（commit 时复用） */
    private Long tempResourceId;

    /** 文件名 */
    private String fileName;

    /** 文件类型 */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** SHA-256 内容哈希 */
    private String contentHash;

    /** 决策类型: NEW / REJECT / SKIP / OVERWRITE */
    private String decision;

    /** 命中维度: NONE / BY_NAME / BY_CONTENT / BOTH */
    private String matchType;

    /** 冲突的旧文档 ID */
    private Long conflictDocumentId;

    /** 冲突的旧文档名称（前端展示用） */
    private String conflictDocumentName;

    /** 拒收原因（仅 REJECT 时返回） */
    private String rejectReason;
}
