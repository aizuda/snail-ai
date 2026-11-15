package com.aizuda.snail.ai.model.common.http;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewApiProviderRequestInterceptorTest {

    @Test
    void shouldRecognizeCanonicalProviderKey() {
        assertTrue(NewApiProviderRequestInterceptor.supports("new-api"));
        assertTrue(NewApiProviderRequestInterceptor.supports(" NEW-API "));
        assertFalse(NewApiProviderRequestInterceptor.supports("newapi"));
        assertFalse(NewApiProviderRequestInterceptor.supports("openai"));
        assertFalse(NewApiProviderRequestInterceptor.supports(null));
    }

    @Test
    void shouldAppendMarkerAndPreserveExistingQueryAndFragment() {
        String marked = NewApiProviderRequestInterceptor.mark(
                "new-api", "https://relay.example/v1/responses?tenant=demo#result");

        assertEquals(
                "https://relay.example/v1/responses?tenant=demo&source=snail-ai#result",
                marked);
    }

    @Test
    void shouldReplaceExistingSourceWithoutDuplicatingMarker() {
        String marked = NewApiProviderRequestInterceptor.mark(
                "new-api", "https://relay.example/v1/chat/completions?source=legacy&source=snail-ai");

        assertEquals(
                "https://relay.example/v1/chat/completions?source=snail-ai",
                marked);
        assertEquals(marked, NewApiProviderRequestInterceptor.mark("new-api", marked));
    }

    @Test
    void shouldLeaveOtherProviderUrlUntouched() {
        String requestUrl = "https://api.openai.com/v1/responses?tenant=demo";

        assertEquals(requestUrl, NewApiProviderRequestInterceptor.mark("openai", requestUrl));
    }

    @Test
    void shouldRewriteRequestAtInterceptorBoundary() throws IOException {
        Request original = new Request.Builder()
                .url("https://relay.example/v1/embeddings?encoding_format=float")
                .build();
        AtomicReference<Request> proceededRequest = new AtomicReference<>();
        Interceptor.Chain chain = recordingChain(original, proceededRequest);

        Response response = NewApiProviderRequestInterceptor.instance().intercept(chain);

        assertNull(response);
        assertEquals(
                "https://relay.example/v1/embeddings?encoding_format=float&source=snail-ai",
                proceededRequest.get().url().toString());
    }

    private Interceptor.Chain recordingChain(Request original, AtomicReference<Request> proceededRequest) {
        return new Interceptor.Chain() {
            @Override
            public Request request() {
                return original;
            }

            @Override
            public Response proceed(Request request) {
                proceededRequest.set(request);
                return null;
            }

            @Override
            public Connection connection() {
                return null;
            }

            @Override
            public Call call() {
                return null;
            }

            @Override
            public int connectTimeoutMillis() {
                return 0;
            }

            @Override
            public Interceptor.Chain withConnectTimeout(int timeout, TimeUnit unit) {
                return this;
            }

            @Override
            public int readTimeoutMillis() {
                return 0;
            }

            @Override
            public Interceptor.Chain withReadTimeout(int timeout, TimeUnit unit) {
                return this;
            }

            @Override
            public int writeTimeoutMillis() {
                return 0;
            }

            @Override
            public Interceptor.Chain withWriteTimeout(int timeout, TimeUnit unit) {
                return this;
            }
        };
    }
}
