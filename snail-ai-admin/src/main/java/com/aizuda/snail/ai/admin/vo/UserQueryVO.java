package com.aizuda.snail.ai.admin.vo;

import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * author: opensnail
 * date: 2025-07-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryVO extends BaseQueryVO {

    private String email;
}
