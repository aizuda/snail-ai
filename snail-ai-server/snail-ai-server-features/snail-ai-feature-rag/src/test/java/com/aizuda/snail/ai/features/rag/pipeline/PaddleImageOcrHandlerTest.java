package com.aizuda.snail.ai.features.rag.pipeline;

import com.aizuda.snail.ai.features.rag.docling.DoclingProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaddleImageOcrHandlerTest {

    private static final String BASE64_IMAGE = Base64.getEncoder()
            .encodeToString("image-bytes".getBytes(StandardCharsets.UTF_8));

    private HttpServer server;
    private String requestBody = "";
    private final List<String> requestBodies = new ArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void recognizePostsBase64ImageAndReturnsRootText() throws IOException {
        startServer("""
                {"text":"发票号码 123"}
                """);
        PaddleImageOcrHandler handler = new PaddleImageOcrHandler(properties());

        String text = handler.recognize(request());

        assertEquals("发票号码 123", text);
        assertTrue(requestBody.contains("\"imageBase64\":\"" + BASE64_IMAGE + "\""));
    }

    @Test
    void recognizeJoinsNestedPaddleTextArrays() throws IOException {
        startServer("""
                {
                  "data": {
                    "rec_texts": ["第一行", "第二行"]
                  }
                }
                """);
        PaddleImageOcrHandler handler = new PaddleImageOcrHandler(properties());

        String text = handler.recognize(request());

        assertEquals("第一行\n第二行", text);
    }

    @Test
    void recognizeReturnsEmptyWhenPaddleOcrIsDisabled() {
        DoclingProperties properties = properties();
        properties.setPaddleOcrEnabled(false);
        PaddleImageOcrHandler handler = new PaddleImageOcrHandler(properties);

        String text = handler.recognize(request());

        assertEquals("", text);
    }

    @Test
    void recognizeBatchPostsImagesAndMapsItemStatuses() throws IOException {
        startServer("/ocr/batch", """
                {
                  "items": [
                    {"id":"1","status":"SUCCESS","text":"alpha"},
                    {"id":"2","status":"EMPTY","text":""},
                    {"id":"3","status":"FAILED","error":"bad image"}
                  ]
                }
                """);
        PaddleImageOcrHandler handler = new PaddleImageOcrHandler(properties());

        List<PaddleImageOcrResult> results = handler.recognizeBatch(List.of(
                request(1),
                request(2),
                request(3)
        ));

        assertEquals(3, results.size());
        assertEquals(PaddleImageOcrResult.Status.SUCCESS, results.get(0).getStatus());
        assertEquals("alpha", results.get(0).getText());
        assertEquals(PaddleImageOcrResult.Status.EMPTY, results.get(1).getStatus());
        assertEquals(PaddleImageOcrResult.Status.FAILED, results.get(2).getStatus());
        assertEquals("bad image", results.get(2).getError());
        assertTrue(requestBody.contains("\"images\""));
        assertTrue(requestBody.contains("\"id\":\"1\""));
        assertTrue(requestBody.contains("\"imageBase64\":\"" + BASE64_IMAGE + "\""));
    }

    @Test
    void recognizeBatchSplitsLargeRequestsByConfiguredBatchSize() throws IOException {
        startServer("/ocr/batch", """
                {
                  "items": [
                    {"id":"1","status":"SUCCESS","text":"alpha"},
                    {"id":"2","status":"SUCCESS","text":"beta"},
                    {"id":"3","status":"SUCCESS","text":"gamma"}
                  ]
                }
                """);
        DoclingProperties properties = properties();
        properties.setPaddleOcrBatchSize(2);
        PaddleImageOcrHandler handler = new PaddleImageOcrHandler(properties);

        List<PaddleImageOcrResult> results = handler.recognizeBatch(List.of(
                request(1),
                request(2),
                request(3)
        ));

        assertEquals(3, results.size());
        assertEquals(2, requestBodies.size());
        assertTrue(requestBodies.get(0).contains("\"id\":\"1\""));
        assertTrue(requestBodies.get(0).contains("\"id\":\"2\""));
        assertTrue(requestBodies.get(1).contains("\"id\":\"3\""));
    }

    @Test
    void recognizeBatchFallsBackToSingleRequestsWhenBatchEndpointIsUnavailable() throws IOException {
        startServer("/ocr", """
                {"text":"single text"}
                """);
        PaddleImageOcrHandler handler = new PaddleImageOcrHandler(properties());

        List<PaddleImageOcrResult> results = handler.recognizeBatch(List.of(request(1), request(2)));

        assertEquals(2, results.size());
        assertEquals(PaddleImageOcrResult.Status.SUCCESS, results.get(0).getStatus());
        assertEquals(PaddleImageOcrResult.Status.SUCCESS, results.get(1).getStatus());
        assertEquals("single text", results.get(0).getText());
    }

    private ImageOcrRequest request() {
        return request(1);
    }

    private ImageOcrRequest request(int imageIndex) {
        return ImageOcrRequest.builder()
                .documentId(1L)
                .imageIndex(imageIndex)
                .base64Image(BASE64_IMAGE)
                .build();
    }

    private DoclingProperties properties() {
        DoclingProperties properties = new DoclingProperties();
        properties.setPaddleOcrEnabled(true);
        if (server != null) {
            properties.setPaddleOcrUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/ocr");
        }
        properties.setPaddleOcrTimeoutMillis(3000);
        properties.setPaddleOcrBatchSize(DoclingProperties.DEFAULT_PADDLE_OCR_BATCH_SIZE);
        return properties;
    }

    private void startServer(String responseBody) throws IOException {
        startServer("/ocr", responseBody);
    }

    private void startServer(String path, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> respond(exchange, responseBody));
        server.start();
    }

    private void respond(HttpExchange exchange, String responseBody) throws IOException {
        requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requestBodies.add(requestBody);
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
