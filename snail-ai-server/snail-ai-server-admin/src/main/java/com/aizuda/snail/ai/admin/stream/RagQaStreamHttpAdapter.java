package com.aizuda.snail.ai.admin.stream;

import com.aizuda.snail.ai.admin.dto.RagQaStreamEvent;
import com.aizuda.snail.ai.common.util.JsonUtil;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Encodes RAG QA events for the fixed HTTP streaming response format.
 */
@Component
public class RagQaStreamHttpAdapter {

    public static final String MEDIA_TYPE = AdminStreamMediaTypes.JSON_TEXT_FRAME;

    public Flux<String> encode(Flux<RagQaStreamEvent> events) {
        return events.map(event -> JsonUtil.toJsonString(event) + "\n");
    }
}
