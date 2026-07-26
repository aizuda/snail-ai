package com.aizuda.snail.ai.admin.vo.memory;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 最近记忆查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemoryRecentQueryVO extends BaseQueryVO {
    /** 最近天数 */
    private Integer days = 7;
    /** 返回数量上限 */
    private Integer limit = 10;
}
