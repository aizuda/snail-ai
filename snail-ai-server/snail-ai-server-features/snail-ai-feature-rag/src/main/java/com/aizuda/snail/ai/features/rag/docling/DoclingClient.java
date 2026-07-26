package com.aizuda.snail.ai.features.rag.docling;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.Semaphore;

/**
 * Docling 服务客户端。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoclingClient {

    private static final String ASYNC_CONVERT_PATH = "/v1/convert/file/async";
    private static final String STATUS_POLL_PATH = "/v1/status/poll/";
    private static final String RESULT_PATH = "/v1/result/";
    private static final String HEALTH_PATH = "/health";
    private static final String TASK_ID_FIELD = "task_id";
    private static final String TASK_STATUS_FIELD = "task_status";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILURE = "failure";
    private static final String FORMAT_MARKDOWN = "md";
    private static final String FORMAT_JSON = "json";
    private static final int MIN_POLL_INTERVAL_MILLIS = 100;
    private static final int MIN_HTTP_TIMEOUT_MILLIS = 1000;
    private static final int MILLIS_PER_SECOND = 1000;

    private final DoclingProperties properties;
    private final DoclingResponseParser responseParser;

    /**
     * 控制提交给外部 Docling Serve 的并发任务数，避免批量解析时压垮单个转换服务。
     */
    private Semaphore parseSemaphore;

    @PostConstruct
    public void init() {
        parseSemaphore = new Semaphore(Math.max(1, properties.getConcurrency()));
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isAvailable() {
        if (!properties.isEnabled()) {
            return false;
        }
        try {
            HttpResponse response = HttpRequest.get(normalizeBaseUrl() + HEALTH_PATH)
                    .timeout(properties.getHealthTimeoutMillis())
                    .execute();
            return response.isOk();
        } catch (Exception e) {
            log.warn("Docling service unavailable: {}", e.getMessage());
            return false;
        }
    }

    public DoclingParseResult convert(File file, DoclingConvertOptions options) {
        if (!properties.isEnabled()) {
            throw new DoclingException("Docling is disabled");
        }
        try {
            parseSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DoclingException("Interrupted while waiting for Docling parse permit", e);
        }

        try {
            long start = System.currentTimeMillis();
            String baseUrl = normalizeBaseUrl();
            int timeoutSeconds = Math.max(1, properties.getTimeoutSeconds());
            String taskId = submitAsyncTask(baseUrl, file, timeoutSeconds, options);
            String status = pollTaskStatus(baseUrl, taskId, timeoutSeconds);
            if (!STATUS_SUCCESS.equals(status)) {
                throw new DoclingException("Docling task failed, taskId=" + taskId + ", status=" + status);
            }
            return fetchTaskResult(baseUrl, taskId, System.currentTimeMillis() - start);
        } finally {
            parseSemaphore.release();
        }
    }

    private String submitAsyncTask(String baseUrl, File file, int timeoutSeconds, DoclingConvertOptions options) {
        // 同时请求 Markdown 和 JSON：Markdown 用于切片，JSON 用于统计、图片和结构化元数据。
        HttpRequest request = HttpRequest.post(baseUrl + ASYNC_CONVERT_PATH)
                .form("files", file)
                .form("to_formats", FORMAT_MARKDOWN)
                .form("to_formats", FORMAT_JSON)
                .timeout(timeoutSeconds * MILLIS_PER_SECOND);
        applyOptions(request, options);

        HttpResponse response = request.execute();
        if (!response.isOk()) {
            throw new DoclingException("Docling task submit failed, httpStatus=" + response.getStatus());
        }
        JSONObject body = JSONUtil.parseObj(response.body());
        String taskId = body.getStr(TASK_ID_FIELD);
        if (StrUtil.isBlank(taskId)) {
            throw new DoclingException("Docling task submit response missing task_id");
        }
        return taskId;
    }

    private void applyOptions(HttpRequest request, DoclingConvertOptions options) {
        // 配置来自知识库 parseParams.docling；未配置时使用项目默认值。
        DoclingConvertOptions effective = options == null ? DoclingConvertOptions.defaults() : options;
        request.form("do_ocr", String.valueOf(effective.isDoOcr()));
        request.form("do_table_structure", String.valueOf(effective.isDoTableStructure()));
        request.form("image_export_mode", effective.getImageExportMode());
        if (StrUtil.isNotBlank(effective.getPdfBackend())) {
            request.form("pdf_backend", effective.getPdfBackend());
        }
        if (CollUtil.isNotEmpty(effective.getOcrLang())) {
            effective.getOcrLang().forEach(lang -> request.form("ocr_lang", lang));
        }
    }

    private String pollTaskStatus(String baseUrl, String taskId, int timeoutSeconds) {
        // Docling 异步接口只返回任务号，这里轮询到 success/failure 或整体超时。
        long deadline = System.currentTimeMillis() + timeoutSeconds * (long) MILLIS_PER_SECOND;
        String url = baseUrl + STATUS_POLL_PATH + taskId;
        int pollIntervalMillis = resolvePollIntervalMillis();
        int statusTimeoutMillis = resolveStatusTimeoutMillis();
        while (System.currentTimeMillis() < deadline) {
            sleepBeforePoll(pollIntervalMillis);
            try {
                HttpResponse response = HttpRequest.get(url).timeout(statusTimeoutMillis).execute();
                if (!response.isOk()) {
                    log.warn("Docling status poll failed, httpStatus={}", response.getStatus());
                    continue;
                }
                String status = JSONUtil.parseObj(response.body()).getStr(TASK_STATUS_FIELD);
                if (STATUS_SUCCESS.equals(status) || STATUS_FAILURE.equals(status)) {
                    return status;
                }
            } catch (Exception e) {
                log.warn("Docling status poll error: {}", e.getMessage());
            }
        }
        return "timeout";
    }

    private void sleepBeforePoll(int pollIntervalMillis) {
        try {
            Thread.sleep(pollIntervalMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DoclingException("Interrupted while polling Docling task", e);
        }
    }

    private DoclingParseResult fetchTaskResult(String baseUrl, String taskId, long costMs) {
        HttpResponse response = HttpRequest.get(baseUrl + RESULT_PATH + taskId)
                .timeout(resolveResultTimeoutMillis())
                .execute();
        if (!response.isOk()) {
            throw new DoclingException("Docling result fetch failed, httpStatus=" + response.getStatus());
        }
        DoclingParseResult result = responseParser.parse(response.body(), costMs);
        if (!result.isSuccess()) {
            throw new DoclingException(result.getErrorMessage());
        }
        return result;
    }

    private String normalizeBaseUrl() {
        String baseUrl = StrUtil.blankToDefault(properties.getUrl(), "http://127.0.0.1:5100").trim();
        return StrUtil.removeSuffix(baseUrl, "/");
    }

    private int resolvePollIntervalMillis() {
        return Math.max(MIN_POLL_INTERVAL_MILLIS, properties.getPollIntervalMillis());
    }

    private int resolveStatusTimeoutMillis() {
        return Math.max(MIN_HTTP_TIMEOUT_MILLIS, properties.getStatusTimeoutMillis());
    }

    private int resolveResultTimeoutMillis() {
        return Math.max(MIN_HTTP_TIMEOUT_MILLIS, properties.getResultTimeoutMillis());
    }
}
