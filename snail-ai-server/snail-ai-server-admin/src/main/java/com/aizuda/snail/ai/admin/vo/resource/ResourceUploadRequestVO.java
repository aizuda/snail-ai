package com.aizuda.snail.ai.admin.vo.resource;

import lombok.Data;

/**
 * 资源上传请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class ResourceUploadRequestVO {
    /** 业务类型 */
    private String bizType;
    /** 业务 ID */
    private Long bizId;
}
