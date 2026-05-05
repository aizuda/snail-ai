package com.aizuda.snail.ai.admin.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationRecordVO {

    private String role;

    private String content;

    private String thinking;

    private Integer status;

    private LocalDateTime createDt;
}
