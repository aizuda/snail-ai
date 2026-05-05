package com.aizuda.snail.ai.admin.vo.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * commit 结果：每个文件的最终落地状态（含 TOCTOU 冲突告警）
 *
 * @author opensnail
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadCommitResultVO {

    /** 是否存在冲突变化（TOCTOU），需要前端再次确认 */
    private Boolean conflictChanged;

    /** 各文件落地结果 */
    private List<RagDocumentResponseVO> items;
}
