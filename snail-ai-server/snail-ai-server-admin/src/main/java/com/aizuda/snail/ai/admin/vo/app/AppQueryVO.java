package com.aizuda.snail.ai.admin.vo.app;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 接入应用分页查询参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppQueryVO extends BaseQueryVO {
    /** 搜索关键字 */
    private String keyword;
    /** 状态 */
    private Integer status;
}
