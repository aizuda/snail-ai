package com.aizuda.snail.ai.admin.vo.memory;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 按类型查询记忆参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemoryTypeQueryVO extends BaseQueryVO {
    /** 记忆类型 */
    private Integer type;
    /** 返回数量上限 */
    private Integer limit = 20;
}
