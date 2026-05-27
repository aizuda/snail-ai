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
 * Agent技能关联持久化对象
 * 表: sai_agent_skill
 *
 * 表示Agent与Skill的多对多关系
 * 一个Agent可以绑定多个Skill，在对话中可调用
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("sai_agent_skill")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentSkillPO {

    /**
     * 关联ID (主键)
     * 自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Agent ID (外键)
     * 关联到 sai_agent.id
     */
    private Long agentId;

    /**
     * Skill ID (外键)
     * 关联到 sai_skill.id
     */
    private Long skillId;

    /**
     * 创建时间
     * 关联关系创建的时刻
     */
    private LocalDateTime createDt;
}
