package com.aizuda.snail.ai.vector.storage.vector.api;

import java.util.Map;
import java.util.function.Function;

/**
 * 向量索引名称构建器：各场景命名规则集中在此，{@link SnailAiVectorStore} 只消费最终字符串。
 */
public enum IndexNameBuilder {

    /**
     * RAG：{@code rag_{ragId}}
     * 参数：{@code ragId}
     */
    KNOWLEDGE("rag", params -> {
        Long ragId = getLong(params, "ragId");
        return "rag_" + ragId;
    }),

    /**
     * 记忆（按 Agent 维度隔离）：{@code memory_agent_{agentId}}
     * 不同 userId 通过 filterExpression 过滤。
     * 参数：{@code agentId}
     */
    MEMORY_AGENT("memory_agent", params -> {
        Long agentId = getLong(params, "agentId");
        return "memory_agent_" + agentId;
    }),

    /**
     * 用户画像（扩展示例）：{@code profile_user_{userId}}
     */
    PROFILE_USER("profile_user", params -> {
        Long userId = getLong(params, "userId");
        return "profile_user_" + userId;
    }),

    /**
     * 会话摘要（扩展示例）：{@code conversation_summary_{conversationId}}
     */
    CONVERSATION_SUMMARY("conversation_summary", params -> {
        Long conversationId = getLong(params, "conversationId");
        return "conversation_summary_" + conversationId;
    });

    private final String type;
    private final Function<Map<String, Object>, String> builder;

    IndexNameBuilder(String type, Function<Map<String, Object>, String> builder) {
        this.type = type;
        this.builder = builder;
    }

    public String getType() {
        return type;
    }

    /**
     * 根据参数 Map 生成索引名称。
     */
    public String build(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException(type + " 参数不能为空");
        }
        return builder.apply(params);
    }

    public static IndexNameBuilder fromType(String type) {
        for (IndexNameBuilder b : values()) {
            if (b.type.equals(type)) {
                return b;
            }
        }
        throw new IllegalArgumentException("未知的索引类型: " + type);
    }

    private static Long getLong(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val == null) {
            throw new IllegalArgumentException("参数 " + key + " 不能为空");
        }
        if (val instanceof Long) {
            return (Long) val;
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof String) {
            return Long.parseLong((String) val);
        }
        throw new IllegalArgumentException("参数 " + key + " 类型无效: " + val.getClass());
    }
}
