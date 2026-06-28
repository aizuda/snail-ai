package com.aizuda.snail.ai.persistence.agent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("sai_user_agent")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAgentPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long agentId;

    private LocalDateTime createDt;
}
