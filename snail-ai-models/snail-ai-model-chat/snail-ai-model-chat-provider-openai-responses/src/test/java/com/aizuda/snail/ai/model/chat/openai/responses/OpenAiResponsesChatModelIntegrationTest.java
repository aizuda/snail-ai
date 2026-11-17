package com.aizuda.snail.ai.model.chat.openai.responses;

import com.aizuda.snail.ai.common.model.ConfigExtAttrsDTO;
import com.aizuda.snail.ai.common.model.HostedToolConfigDTO;
import com.aizuda.snail.ai.model.chat.ChatModelSpec;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.part.ReasoningPart;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenAI Responses HTTP/SSE and local tool-loop integration tests.
 *
 * @author opensnail
 * @since 2026-11-14
 */
class OpenAiResponsesChatModelIntegrationTest {

    private MockResponsesServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockResponsesServer();
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void shouldSendMultimodalHostedToolsAndReadReasoningUsage() {
        String output = "[" + reasoningItem("reasoning_sync", "Inspect the image")
                + "," + messageItem("message_sync", "A small test image") + "]";
        server.enqueueJson(completedResponse("response_sync", output, usage(19, 7, 26, 5)));

        ConfigExtAttrsDTO config = responsesConfig();
        config.setHostedTools(hostedTools());
        config.setExtraBody(Map.of("custom_parameter", "custom-value"));
        ChatModel chatModel = createModel(config);
        Media image = Media.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .data(new byte[]{1, 2, 3})
                .build();
        UserMessage userMessage = UserMessage.builder()
                .text("Describe this image")
                .media(image)
                .build();

        ChatResponse response = chatModel.call(new Prompt(userMessage));

        assertEquals("A small test image", response.getResult().getOutput().getText());
        List<ReasoningPart> reasoning = response.getResult().getOutput().getReasoning();
        assertEquals(1, reasoning.size());
        assertEquals("Inspect the image", reasoning.getFirst().summary());
        Usage responseUsage = response.getMetadata().getUsage();
        assertEquals(19, responseUsage.getPromptTokens());
        assertEquals(7, responseUsage.getCompletionTokens());
        assertEquals(5L, responseUsage.getCacheReadInputTokens());

        CapturedRequest request = server.requests().getFirst();
        assertEquals("/v1/responses", request.path());
        assertTrue(request.body().contains("\"model\":\"gpt-test\""));
        assertTrue(request.body().contains("\"max_output_tokens\":4096"));
        assertTrue(request.body().contains("\"effort\":\"high\""));
        assertTrue(request.body().contains("\"summary\":\"detailed\""));
        assertTrue(request.body().contains("\"verbosity\":\"low\""));
        assertTrue(request.body().contains("\"type\":\"input_image\""));
        assertTrue(request.body().contains("data:image/png;base64,AQID"));
        assertFalse(request.body().contains("\"type\":\"web_search\""));
        assertFalse(request.body().contains("\"type\":\"file_search\""));
        assertTrue(request.body().contains("\"type\":\"code_interpreter\""));
        assertTrue(request.body().contains("\"type\":\"image_generation\""));
        assertTrue(request.body().contains("\"type\":\"local_shell\""));
        assertTrue(request.body().contains("\"custom_parameter\":\"custom-value\""));
        assertFalse(request.body().contains("chat/completions"));
    }

    @Test
    void shouldStreamTextReasoningAndUsage() {
        String output = "[" + reasoningItem("reasoning_stream", "Check facts")
                + "," + messageItem("message_stream", "Hello world") + "]";
        String created = response("response_stream", "in_progress", "[]", "null");
        String completed = completedResponse("response_stream", output, usage(11, 4, 15, 3));
        String events = sse("response.created", "{\"type\":\"response.created\","
                + "\"sequence_number\":0,\"response\":" + created + "}")
                + sse("response.reasoning_summary_text.delta", "{"
                + "\"type\":\"response.reasoning_summary_text.delta\","
                + "\"sequence_number\":1,\"item_id\":\"reasoning_stream\","
                + "\"output_index\":0,\"summary_index\":0,\"delta\":\"Check facts\"}")
                + sse("response.output_text.delta", "{\"type\":\"response.output_text.delta\","
                + "\"sequence_number\":2,\"item_id\":\"message_stream\","
                + "\"output_index\":1,\"content_index\":0,\"logprobs\":[],"
                + "\"delta\":\"Hello world\"}")
                + sse("response.completed", "{\"type\":\"response.completed\","
                + "\"sequence_number\":3,\"response\":" + completed + "}");
        server.enqueueSse(events);

        List<ChatResponse> chunks = createModel(responsesConfig())
                .stream(new Prompt("stream test"))
                .collectList()
                .block();

        assertNotNull(chunks);
        StringBuilder text = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        for (ChatResponse chunk : chunks) {
            if (chunk.getResult() == null) {
                continue;
            }
            String chunkText = chunk.getResult().getOutput().getText();
            if (chunkText != null) {
                text.append(chunkText);
            }
            for (ReasoningPart part : chunk.getResult().getOutput().getReasoning()) {
                if (part.summary() != null) {
                    reasoning.append(part.summary());
                }
            }
        }
        assertEquals("Hello world", text.toString());
        assertEquals("Check facts", reasoning.toString());
        Usage terminalUsage = chunks.getLast().getMetadata().getUsage();
        assertEquals(11, terminalUsage.getPromptTokens());
        assertEquals(3L, terminalUsage.getCacheReadInputTokens());
        assertTrue(server.requests().getFirst().body().contains("\"stream\":true"));
    }

    @Test
    void shouldMarkNewApiRequestUrl() {
        server.enqueueJson(completedResponse(
                "response_new_api",
                "[" + messageItem("message_new_api", "New API response") + "]",
                usage(4, 3, 7, 0)));

        ChatResponse response = createModel(responsesConfig(), "new-api")
                .call(new Prompt("new api marker test"));

        assertEquals("New API response", response.getResult().getOutput().getText());
        CapturedRequest request = server.requests().getFirst();
        assertEquals("/v1/responses", request.path());
        assertEquals("source=snail-ai", request.query());
    }

    @Test
    void shouldRoundTripLocalFunctionSkillAndMcpCallbacksWithoutFallback() {
        server.enqueueJson(completedResponse("response_tool_1",
                "[" + functionCallItem("local_function", "call_local") + "]", usage(5, 2, 7, 0)));
        server.enqueueJson(completedResponse("response_tool_2",
                "[" + functionCallItem("skill_tool", "call_skill") + "]", usage(8, 2, 10, 0)));
        server.enqueueJson(completedResponse("response_tool_3",
                "[" + functionCallItem("mcp_tool", "call_mcp") + "]", usage(11, 2, 13, 0)));
        server.enqueueJson(completedResponse("response_tool_4",
                "[" + messageItem("message_tool", "All local tools completed") + "]", usage(14, 4, 18, 1)));

        List<String> invocations = new CopyOnWriteArrayList<>();
        ToolCallback[] callbacks = {
                recordingTool("local_function", invocations),
                recordingTool("skill_tool", invocations),
                recordingTool("mcp_tool", invocations)
        };
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();
        ChatClient chatClient = ChatClient.builder(createModel(responsesConfig()))
                .defaultAdvisors(ToolCallingAdvisor.builder()
                        .toolCallingManager(toolCallingManager)
                        .build())
                .defaultTools((Object[]) callbacks)
                .build();

        String content = chatClient.prompt("Run the configured local tools").call().content();

        assertEquals("All local tools completed", content);
        assertEquals(List.of("local_function", "skill_tool", "mcp_tool"), invocations);
        assertEquals(4, server.requests().size());
        assertTrue(server.requests().get(1).body().contains("\"type\":\"function_call_output\""));
        assertTrue(server.requests().get(1).body().contains("\"call_id\":\"call_local\""));
        assertTrue(server.requests().get(2).body().contains("\"call_id\":\"call_skill\""));
        assertTrue(server.requests().get(3).body().contains("\"call_id\":\"call_mcp\""));
        assertTrue(server.requests().stream().allMatch(request -> "/v1/responses".equals(request.path())));
    }

    private ChatModel createModel(ConfigExtAttrsDTO config) {
        return createModel(config, "openai");
    }

    private ChatModel createModel(ConfigExtAttrsDTO config, String providerKey) {
        ChatModelSpec spec = new ChatModelSpec(
                OpenAiResponsesChatModelAdapter.ADAPTER_KEY,
                providerKey,
                server.baseUrl(),
                "test-api-key",
                "gpt-test",
                config);
        return new OpenAiResponsesChatModelAdapter().create(spec);
    }

    private ConfigExtAttrsDTO responsesConfig() {
        ConfigExtAttrsDTO config = new ConfigExtAttrsDTO();
        config.setMaxRetries(0);
        config.setMaxTokens(4096);
        config.setTemperature(0.2);
        config.setTopP(0.9);
        config.setReasoningEffort("high");
        config.setReasoningSummary("detailed");
        config.setVerbosity("low");
        config.setToolChoice("auto");
        config.setParallelToolCalls(true);
        config.setMaxToolCalls(6);
        config.setResponseFormat("json_object");
        return config;
    }

    private List<HostedToolConfigDTO> hostedTools() {
        List<HostedToolConfigDTO> tools = new ArrayList<>();

        HostedToolConfigDTO codeInterpreter = new HostedToolConfigDTO();
        codeInterpreter.setType("code_interpreter");
        tools.add(codeInterpreter);

        HostedToolConfigDTO imageGeneration = new HostedToolConfigDTO();
        imageGeneration.setType("image_generation");
        imageGeneration.setModel("gpt-image-1");
        imageGeneration.setSize("1024x1024");
        tools.add(imageGeneration);

        HostedToolConfigDTO raw = new HostedToolConfigDTO();
        raw.setType("raw");
        raw.setTool(Map.of("type", "local_shell"));
        tools.add(raw);

        return tools;
    }

    private ToolCallback recordingTool(String name, List<String> invocations) {
        ToolDefinition definition = ToolDefinition.builder()
                .name(name)
                .description("Test callback " + name)
                .inputSchema("{\"type\":\"object\",\"properties\":{\"value\":{\"type\":\"string\"}}}")
                .build();
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public String call(String input) {
                invocations.add(name);
                return name + " result";
            }
        };
    }

    private static String completedResponse(String id, String output, String usage) {
        return response(id, "completed", output, usage);
    }

    private static String response(String id, String status, String output, String usage) {
        return """
                {"id":"%s","object":"response","created_at":1.0,"error":null,
                "incomplete_details":null,"instructions":null,"metadata":{},"model":"gpt-test",
                "output":%s,"parallel_tool_calls":true,"temperature":null,"tool_choice":"auto",
                "tools":[],"top_p":null,"status":"%s","usage":%s}
                """.formatted(id, output, status, usage).replace("\n", "");
    }

    private static String reasoningItem(String id, String summary) {
        return "{\"id\":\"" + id + "\",\"type\":\"reasoning\",\"status\":\"completed\","
                + "\"summary\":[{\"type\":\"summary_text\",\"text\":\"" + summary + "\"}],"
                + "\"encrypted_content\":\"encrypted-test-content\"}";
    }

    private static String messageItem(String id, String text) {
        return "{\"id\":\"" + id + "\",\"type\":\"message\",\"status\":\"completed\","
                + "\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\","
                + "\"annotations\":[],\"text\":\"" + text + "\"}]}";
    }

    private static String functionCallItem(String name, String callId) {
        return "{\"id\":\"item_" + callId + "\",\"type\":\"function_call\","
                + "\"status\":\"completed\",\"call_id\":\"" + callId + "\","
                + "\"name\":\"" + name + "\","
                + "\"arguments\":\"{\\\"value\\\":\\\"test\\\"}\"}";
    }

    private static String usage(int inputTokens, int outputTokens, int totalTokens, int cachedTokens) {
        return "{\"input_tokens\":" + inputTokens
                + ",\"input_tokens_details\":{\"cache_write_tokens\":0,\"cached_tokens\":" + cachedTokens + "}"
                + ",\"output_tokens\":" + outputTokens
                + ",\"output_tokens_details\":{\"reasoning_tokens\":1}"
                + ",\"total_tokens\":" + totalTokens + "}";
    }

    private static String sse(String event, String data) {
        return "event: " + event + "\n" + "data: " + data + "\n\n";
    }

    private record StubResponse(String contentType, String body) {
    }

    private record CapturedRequest(String path, String query, String body) {
    }

    private static final class MockResponsesServer implements AutoCloseable {

        private final Queue<StubResponse> responses = new ConcurrentLinkedQueue<>();
        private final List<CapturedRequest> requests = new CopyOnWriteArrayList<>();
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private HttpServer httpServer;

        void start() throws IOException {
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            httpServer.createContext("/", this::handle);
            httpServer.setExecutor(executor);
            httpServer.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/v1";
        }

        List<CapturedRequest> requests() {
            return requests;
        }

        void enqueueJson(String body) {
            responses.add(new StubResponse("application/json", body));
        }

        void enqueueSse(String body) {
            responses.add(new StubResponse("text/event-stream", body));
        }

        private void handle(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getRawQuery();
            requests.add(new CapturedRequest(path, query, body));
            if (!"/v1/responses".equals(path)) {
                respond(exchange, 404, "application/json", "{\"error\":\"unexpected path\"}");
                return;
            }
            StubResponse response = responses.poll();
            if (response == null) {
                respond(exchange, 500, "application/json", "{\"error\":\"missing stub\"}");
                return;
            }
            respond(exchange, 200, response.contentType(), response.body());
        }

        private void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        @Override
        public void close() {
            if (httpServer != null) {
                httpServer.stop(0);
            }
            executor.shutdownNow();
        }
    }
}
