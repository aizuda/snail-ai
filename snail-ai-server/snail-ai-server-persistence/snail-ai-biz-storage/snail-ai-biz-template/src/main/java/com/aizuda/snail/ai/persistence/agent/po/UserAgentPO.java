package com.aizuda.snail.ai.persistence.agent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户与智能体的关联关系（用户添加 / 收藏的智能体）。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@TableName("sai_user_agent")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAgentPO {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 智能体 ID */
    private Long agentId;

    /** 创建时间 */
    private LocalDateTime createDt;
}
