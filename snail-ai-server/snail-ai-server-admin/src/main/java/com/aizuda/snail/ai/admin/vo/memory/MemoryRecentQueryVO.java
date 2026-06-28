package com.aizuda.snail.ai.admin.vo.memory;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MemoryRecentQueryVO
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemoryRecentQueryVO extends BaseQueryVO {
    private Integer days = 7;
    private Integer limit = 10;
}
