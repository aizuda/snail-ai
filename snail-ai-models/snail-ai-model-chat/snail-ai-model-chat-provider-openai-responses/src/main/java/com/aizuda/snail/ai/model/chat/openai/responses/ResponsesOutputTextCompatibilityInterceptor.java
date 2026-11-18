package com.aizuda.snail.ai.model.chat.openai.responses;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;
import okio.Source;
import okio.Timeout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 在 Responses 响应进入 OpenAI SDK 之前补齐缺失的 output_text.annotations。
 *
 * @author opensnail
 * @since 2026-11-18
 */
final class ResponsesOutputTextCompatibilityInterceptor implements Interceptor {

    private static final ByteString EVENT_SEPARATOR = ByteString.encodeUtf8("\n\n");
    private static final ByteString EVENT_SEPARATOR_CRLF = ByteString.encodeUtf8("\r\n\r\n");
    private static final String EVENT_STREAM = "event-stream";
    private static final String JSON = "json";

    private static final ResponsesOutputTextCompatibilityInterceptor INSTANCE =
            new ResponsesOutputTextCompatibilityInterceptor();

    private ResponsesOutputTextCompatibilityInterceptor() {
    }

    static ResponsesOutputTextCompatibilityInterceptor instance() {
        return INSTANCE;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        ResponseBody body = response.body();
        if (body == null) {
            return response;
        }
        MediaType mediaType = body.contentType();
        if (isEventStream(mediaType)) {
            Source normalized = new EventStreamSource(body.source());
            return response.newBuilder()
                    .body(new StreamingBody(mediaType, Okio.buffer(normalized)))
                    .build();
        }
        if (!isJson(mediaType)) {
            return response;
        }
        String normalized = ResponsesOutputTextNormalizer.normalize(body.string());
        return response.newBuilder()
                .body(new BufferedBody(mediaType, normalized))
                .build();
    }

    private static boolean isEventStream(MediaType mediaType) {
        return mediaType != null && mediaType.toString().toLowerCase(Locale.ROOT).contains(EVENT_STREAM);
    }

    private static boolean isJson(MediaType mediaType) {
        return mediaType != null && mediaType.toString().toLowerCase(Locale.ROOT).contains(JSON);
    }

    private static final class BufferedBody extends ResponseBody {

        private final MediaType mediaType;
        private final Buffer buffer;
        private final long contentLength;

        private BufferedBody(MediaType mediaType, String content) {
            this.mediaType = mediaType;
            this.buffer = new Buffer().writeString(content, StandardCharsets.UTF_8);
            this.contentLength = buffer.size();
        }

        @Override
        public MediaType contentType() {
            return mediaType;
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        @Override
        public BufferedSource source() {
            return buffer.clone();
        }
    }

    private static final class StreamingBody extends ResponseBody {

        private final MediaType mediaType;
        private final BufferedSource source;

        private StreamingBody(MediaType mediaType, BufferedSource source) {
            this.mediaType = mediaType;
            this.source = source;
        }

        @Override
        public MediaType contentType() {
            return mediaType;
        }

        @Override
        public long contentLength() {
            return -1;
        }

        @Override
        public BufferedSource source() {
            return source;
        }
    }

    static final class EventStreamSource implements Source {

        private final Source delegate;
        private final Buffer pending = new Buffer();
        private final Buffer ready = new Buffer();
        private boolean exhausted;

        EventStreamSource(Source delegate) {
            this.delegate = delegate;
        }

        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
            if (byteCount < 0) {
                throw new IllegalArgumentException("byteCount < 0: " + byteCount);
            }
            if (byteCount == 0) {
                return 0;
            }
            fill(byteCount);
            if (ready.size() == 0) {
                return exhausted ? -1 : 0;
            }
            return ready.read(sink, Math.min(byteCount, ready.size()));
        }

        @Override
        public Timeout timeout() {
            return delegate.timeout();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        private void fill(long byteCount) throws IOException {
            while (!exhausted && ready.size() < byteCount) {
                long read = delegate.read(pending, Math.max(byteCount, 8192));
                if (read == -1) {
                    exhausted = true;
                    emit(true);
                    return;
                }
                if (read == 0) {
                    return;
                }
                emit(false);
            }
        }

        private void emit(boolean flushTail) throws IOException {
            while (true) {
                long lf = pending.indexOf(EVENT_SEPARATOR);
                long crlf = pending.indexOf(EVENT_SEPARATOR_CRLF);
                ByteString separator = selectSeparator(lf, crlf);
                if (separator == null) {
                    break;
                }
                long index = separator == EVENT_SEPARATOR_CRLF ? crlf : lf;
                String event = pending.readString(index, StandardCharsets.UTF_8);
                pending.skip(separator.size());
                ready.writeString(ResponsesOutputTextNormalizer.normalizeSseEvent(event), StandardCharsets.UTF_8);
                ready.write(separator);
            }
            if (flushTail && pending.size() > 0) {
                String tail = pending.readString(pending.size(), StandardCharsets.UTF_8);
                ready.writeString(ResponsesOutputTextNormalizer.normalizeSseEvent(tail), StandardCharsets.UTF_8);
            }
        }

        private static ByteString selectSeparator(long lf, long crlf) {
            if (lf < 0 && crlf < 0) {
                return null;
            }
            if (lf < 0) {
                return EVENT_SEPARATOR_CRLF;
            }
            if (crlf < 0 || lf <= crlf) {
                return EVENT_SEPARATOR;
            }
            return EVENT_SEPARATOR_CRLF;
        }
    }
}
