package com.aizuda.snail.ai.agent.common.rpc;

import com.aizuda.snail.ai.agent.common.rpc.annotation.Mapping;
import com.aizuda.snail.ai.common.dto.agent.ConversationCreateRequest;
import com.aizuda.snail.ai.common.dto.agent.ConversationRecordRequest;
import com.aizuda.snail.ai.common.dto.agent.SkillContentRequest;
import com.aizuda.snail.ai.common.dto.agent.SkillContentResponse;
import com.aizuda.snail.ai.common.dto.memory.ShortTermMemoryRequest;
import com.aizuda.snail.ai.common.dto.rag.RagSearchRequest;
import com.aizuda.snail.ai.common.grpc.constant.UriConstants;

import java.util.List;
import java.util.Map;



/**
 * 请求Server接口
 *
 * @author opensnail
 * @date 2025-04-12
 */
public interface RpcClient {
    
    /**
     * 创建对话并保存用户消息
     */
    @Mapping(path = UriConstants.CALLBACK_CONVERSATION_CREATE)
    void createConversation(ConversationCreateRequest request);
    
    /**
     * 保存对话消息记录
     */
    @Mapping(path = UriConstants.CALLBACK_CONVERSATION_RECORD)
    void saveRecord(ConversationRecordRequest request);

    /**
     * 加载短期对话历史
     */
    @Mapping(path = UriConstants.CALLBACK_MEMORY_SHORT_TERM)
    List<Map<String, Object>> loadShortTermHistory(ShortTermMemoryRequest request);

    /**
     * 获取 Skill 完整内容
     */
    @Mapping(path = UriConstants.CALLBACK_SKILL_CONTENT)
    SkillContentResponse fetchSkillContent(SkillContentRequest request);

    /**
     * 知识库搜索
     */
    @Mapping(path = UriConstants.CALLBACK_RAG_SEARCH, timeout = 120_000)
    com.aizuda.snail.ai.common.dto.rag.RagSearchResponse searchRag(RagSearchRequest request);


}
