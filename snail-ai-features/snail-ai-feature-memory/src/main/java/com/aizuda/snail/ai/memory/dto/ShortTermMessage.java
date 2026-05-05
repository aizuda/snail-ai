package com.aizuda.snail.ai.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 短期记忆消息 - 用于在各存储介质中传递会话历史
 *
 * author: opensnail
 * date: 2026-03-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortTermMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息角色：user / assistant */
    private String role;

    /** 消息内容 */
    private String content;
}
