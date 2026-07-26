package com.aizuda.snail.ai.admin.vo.agent;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AgentChatRequestVOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void testShouldDeserializeImageAttachments() throws Exception {
        String json = """
                {
                  "conversationId": "conv-1",
                  "content": "看看这张图",
                  "attachments": [
                    {
                      "resourceId": 123,
                      "type": "IMAGE"
                    }
                  ]
                }
                """;

        AgentChatRequestVO request = objectMapper.readValue(json, AgentChatRequestVO.class);

        assertEquals("conv-1", request.getConversationId());
        assertEquals("看看这张图", request.getContent());
        assertNotNull(request.getAttachments());
        assertEquals(1, request.getAttachments().size());
        assertEquals(123L, request.getAttachments().get(0).getResourceId());
        assertEquals("IMAGE", request.getAttachments().get(0).getType());
    }

    private void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private void assertNotNull(Object actual) {
        if (actual == null) {
            throw new AssertionError("Expected value to be non-null");
        }
    }
}
