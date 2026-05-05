package com.aizuda.snail.ai.admin.vo.rag;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * commit 时单文件最终决策（前端可逐行覆盖）
 *
 * @author opensnail
 */
@Data
public class UploadCommitItemVO {

    @NotNull(message = "tempResourceId is required")
    private Long tempResourceId;

    /** 用户最终选定的决策: NEW / SKIP / OVERWRITE（REJECT 不能由 commit 提交） */
    @NotNull(message = "decision is required")
    private String decision;
}
