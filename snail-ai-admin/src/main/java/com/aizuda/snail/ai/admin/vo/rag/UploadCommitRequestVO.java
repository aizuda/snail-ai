package com.aizuda.snail.ai.admin.vo.rag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * commit 请求：凭 previewToken + 用户对每条记录的最终决策提交入库
 *
 * @author opensnail
 */
@Data
public class UploadCommitRequestVO {

    @NotBlank(message = "previewToken is required")
    private String previewToken;

    @NotEmpty(message = "items is required")
    @Valid
    private List<UploadCommitItemVO> items;
}
