package com.aizuda.snail.ai.admin.stream;

import com.aizuda.snail.ai.admin.dto.AgentCreateStreamEvent;
import com.aizuda.snail.ai.common.util.JsonUtil;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Encodes agent creation events for the fixed HTTP streaming response format.
 */
@Component
public class AgentCreateStreamHttpAdapter {

    public static final String MEDIA_TYPE = AdminStreamMediaTypes.JSON_TEXT_FRAME;

    public Flux<String> encode(Flux<AgentCreateStreamEvent> events) {
        return events.map(event -> JsonUtil.toJsonString(event) + "\n");
    }
}
