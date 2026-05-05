package com.aizuda.snail.ai.agent.core.advisor;

/**
 * {@link org.springframework.ai.chat.client.ChatClientRequest#context()} 中使用的参数键。
 */
public final class ClientAdvisorKeys {

    /** {@link com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest} */
    public static final String DISPATCH = "snail.dispatch";

    /** {@link com.aizuda.snail.ai.agent.common.report.ClientTraceBuffer}（可选，供拦截器使用） */
    public static final String TRACE_BUFFER = "snail.traceBuffer";

    public static final String STREAM_STATE = "snail.streamState";

    /** java.util.function.Consumer&lt;String&gt; 文本 chunk */
    public static final String CHUNK_CONSUMER = "snail.chunkConsumer";

    /** java.util.function.Consumer&lt;String&gt; 思考 chunk */
    public static final String THINKING_CONSUMER = "snail.thinkingConsumer";

    private ClientAdvisorKeys() {
    }
}
