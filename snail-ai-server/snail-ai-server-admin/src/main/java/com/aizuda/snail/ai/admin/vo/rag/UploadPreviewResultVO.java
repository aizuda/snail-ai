package com.aizuda.snail.ai.admin.vo.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 上传预览返回：token + 各文件预测结果
 *
 * @author opensnail
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadPreviewResultVO {

    /** 预览 token，commit/cancel 时回传 */
    private String previewToken;

    /** RAG ID */
    private Long ragId;

    /** 各文件预测结果（顺序与上传顺序一致） */
    private List<UploadPreviewItemVO> items;
}
