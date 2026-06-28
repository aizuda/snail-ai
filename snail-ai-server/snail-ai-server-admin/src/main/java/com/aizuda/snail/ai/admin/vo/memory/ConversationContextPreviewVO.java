package com.aizuda.snail.ai.admin.vo.memory;

import com.aizuda.snail.ai.memory.dto.ConversationMemoryDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 下一轮对话请求将带入的历史与记忆预览（粗算 token，不写访问日志） */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationContextPreviewVO {

    private int historyMessageCount;
    private List<String> historyPreviewLines;
    private List<ConversationMemoryDTO> memories;
    private int estimatedPromptTokens;
    private boolean compressionApplied;
}
