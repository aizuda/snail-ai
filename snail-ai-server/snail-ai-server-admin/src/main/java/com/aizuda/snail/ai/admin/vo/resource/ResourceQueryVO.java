package com.aizuda.snail.ai.admin.vo.resource;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资源分页查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceQueryVO extends BaseQueryVO {
    /** 业务类型 */
    private String bizType;
    /** 原始文件名 */
    private String originalName;
    /** 业务 ID */
    private Long bizId;
}
