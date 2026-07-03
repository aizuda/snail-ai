package com.aizuda.snail.ai.features.rag.docling;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Docling 服务配置。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@Data
@Component
@ConfigurationProperties(prefix = "snail-ai.rag.docling")
public class DoclingProperties {

    public static final int DEFAULT_POLL_INTERVAL_MILLIS = 3000;
    public static final int DEFAULT_STATUS_TIMEOUT_MILLIS = 10000;
    public static final int DEFAULT_RESULT_TIMEOUT_MILLIS = 30000;
    public static final int DEFAULT_MAX_IMAGE_COUNT = 100;
    public static final long DEFAULT_MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    public static final int DEFAULT_PADDLE_OCR_TIMEOUT_MILLIS = 180000;
    public static final int DEFAULT_PADDLE_OCR_BATCH_SIZE = 5;

    /**
     * 是否启用 Docling 解析能力；默认关闭，关闭时始终使用项目内置解析器。
     */
    private boolean enabled;

    /**
     * Docling Serve 基础地址。
     */
    private String url = "http://127.0.0.1:5100";

    /**
     * 单个解析任务的整体超时时间，单位秒。
     */
    private int timeoutSeconds = 300;

    /**
     * 当前应用实例允许同时提交给 Docling 的最大任务数。
     */
    private int concurrency = 3;

    /**
     * 健康检查超时时间，单位毫秒。
     */
    private int healthTimeoutMillis = 3000;

    /**
     * Docling 异步任务状态轮询间隔，单位毫秒；调小可更快感知完成，调大可降低 Docling Serve 压力。
     */
    private int pollIntervalMillis = DEFAULT_POLL_INTERVAL_MILLIS;

    /**
     * 单次状态轮询 HTTP 超时，单位毫秒，避免状态接口卡住整个解析线程。
     */
    private int statusTimeoutMillis = DEFAULT_STATUS_TIMEOUT_MILLIS;

    /**
     * 任务成功后拉取解析结果的 HTTP 超时，单位毫秒，适配大文档结果体。
     */
    private int resultTimeoutMillis = DEFAULT_RESULT_TIMEOUT_MILLIS;

    /**
     * 单个文档最多保存的 Docling 图片数，避免扫描型 PDF 一次落库过多资源。
     */
    private int maxImageCount = DEFAULT_MAX_IMAGE_COUNT;

    /**
     * 单张 Docling 图片最大字节数，超过时跳过保存，避免 base64 图片占用过多内存和存储。
     */
    private long maxImageBytes = DEFAULT_MAX_IMAGE_BYTES;

    /**
     * 是否启用 PaddleOCR 作为 Docling 图片 OCR 主引擎。
     */
    private boolean paddleOcrEnabled;

    /**
     * PaddleOCR HTTP 服务地址，接收 {"imageBase64":"..."} 并返回 OCR 文本。
     */
    private String paddleOcrUrl = "http://127.0.0.1:8866/ocr";

    /**
     * PaddleOCR HTTP 调用超时时间，单位毫秒。
     */
    private int paddleOcrTimeoutMillis = DEFAULT_PADDLE_OCR_TIMEOUT_MILLIS;

    /**
     * PaddleOCR 单次批量请求的最大图片数。PaddleOCR CPU 推理较慢，大批次会导致整批 HTTP 读超时。
     */
    private int paddleOcrBatchSize = DEFAULT_PADDLE_OCR_BATCH_SIZE;

    /**
     * PaddleOCR 无结果时是否启用多模态模型兜底。
     */
    private boolean visionOcrFallbackEnabled;
}
