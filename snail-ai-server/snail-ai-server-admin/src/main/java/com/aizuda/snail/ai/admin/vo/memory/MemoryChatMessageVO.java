package com.aizuda.snail.ai.admin.vo.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记忆对话消息对象
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryChatMessageVO {

    /** 消息角色（user/assistant/system） */
    private String role;
    /** 消息内容 */
    private String content;
}
