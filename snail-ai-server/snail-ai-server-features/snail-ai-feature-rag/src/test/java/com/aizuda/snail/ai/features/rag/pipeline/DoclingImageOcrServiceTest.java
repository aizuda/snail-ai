package com.aizuda.snail.ai.features.rag.pipeline;

import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.service.ModelRuntimeHandler;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DoclingImageOcrServiceTest {

    private static final long MODEL_CONFIG_ID = 6L;
    private static final byte[] IMAGE_BYTES = "image-bytes".getBytes(StandardCharsets.UTF_8);
    private static final String BASE64_IMAGE = Base64.getEncoder().encodeToString(IMAGE_BYTES);

    @Test
    void recognizeSendsImageAsMediaInsteadOfTextDataUri() {
        CapturingModelRuntimeHandler handler = new CapturingModelRuntimeHandler("识别结果");
        DoclingImageOcrService service = new DoclingImageOcrService(handler);

        String result = service.recognize(BASE64_IMAGE, MODEL_CONFIG_ID);

        assertEquals("识别结果", result);
        assertEquals(MODEL_CONFIG_ID, handler.request.modelConfigId());
        assertArrayEquals(IMAGE_BYTES, handler.request.mediaBytes());
        assertEquals("image/png", handler.request.mimeType());
        org.junit.jupiter.api.Assertions.assertFalse(handler.request.userContext().contains("data:image"));
        org.junit.jupiter.api.Assertions.assertFalse(handler.request.userContext().contains(BASE64_IMAGE));
    }

    @Test
    void recognizeThrowsQuotaExceededWhenModelReportsQuotaLimit() {
        CapturingModelRuntimeHandler handler = new CapturingModelRuntimeHandler(
                new ModelCallException(ModelCallException.ErrorCode.MODEL_CALL_FAILED,
                        "429: You exceeded your current quota; token-limit"));
        DoclingImageOcrService service = new DoclingImageOcrService(handler);

        assertThrows(DoclingImageOcrQuotaExceededException.class,
                () -> service.recognize(BASE64_IMAGE, MODEL_CONFIG_ID));
    }

    private static class CapturingModelRuntimeHandler extends ModelRuntimeHandler {

        private final String result;
        private final RuntimeException failure;
        private ModelRuntimeHandler.ChatMediaRequest request;

        CapturingModelRuntimeHandler(String result) {
            super(null, null, null, null);
            this.result = result;
            this.failure = null;
        }

        CapturingModelRuntimeHandler(RuntimeException failure) {
            super(null, null, null, null);
            this.result = null;
            this.failure = failure;
        }

        @Override
        public String chat(ModelRuntimeHandler.ChatMediaRequest request) {
            this.request = request;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }
}
