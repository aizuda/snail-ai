package com.aizuda.snail.ai.admin.vo.memory;

import lombok.Data;

/**
 * 更新记忆请求参数
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
public class UpdateMemoryRequestVO {
    /** 记忆内容 */
    private String content;
}
