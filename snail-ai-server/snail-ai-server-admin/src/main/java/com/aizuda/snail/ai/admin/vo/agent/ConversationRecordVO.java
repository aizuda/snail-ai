package com.aizuda.snail.ai.admin.vo.agent;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
