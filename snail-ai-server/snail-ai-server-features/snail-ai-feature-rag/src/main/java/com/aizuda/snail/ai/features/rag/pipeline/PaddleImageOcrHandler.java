package com.aizuda.snail.ai.features.rag.pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import com.aizuda.snail.ai.features.rag.docling.DoclingProperties;

/**
 * PaddleOCR HTTP 图片 OCR 处理器。
 *
 * @author opensnail
 * @since 2026-07-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaddleImageOcrHandler implements ImageOcrHandler {

    private static final int DEFAULT_TIMEOUT_MILLIS = DoclingProperties.DEFAULT_PADDLE_OCR_TIMEOUT_MILLIS;
    private static final int DEFAULT_BATCH_SIZE = DoclingProperties.DEFAULT_PADDLE_OCR_BATCH_SIZE;
    private static final String IMAGE_BASE64_FIELD = "imageBase64";
    private static final String TEXT_FIELD = "text";
    private static final String DATA_FIELD = "data";
    private static final String ID_FIELD = "id";
    private static final String IMAGES_FIELD = "images";
    private static final String STATUS_FIELD = "status";
    private static final String ERROR_FIELD = "error";
    private static final String RESULTS_FIELD = "results";
    private static final String RESULT_FIELD = "result";
    private static final String REC_TEXTS_FIELD = "rec_texts";
    private static final String OCR_TEXT_FIELD = "ocrText";
    private static final String CONTENT_FIELD = "content";
    private static final String BATCH_PATH_SUFFIX = "/batch";
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;

    private final DoclingProperties doclingProperties;

    @Override
    public String recognize(ImageOcrRequest request) {
        PaddleImageOcrResult result = recognizeSingleResult(request);
        return result.getStatus() == PaddleImageOcrResult.Status.SUCCESS ? result.getText() : "";
    }

    @Override
    public List<PaddleImageOcrResult> recognizeBatch(List<ImageOcrRequest> requests) {
        if (!isEnabled() || CollUtil.isEmpty(requests)) {
            return List.of();
        }
        List<ImageOcrRequest> validRequests = requests.stream()
                .filter(request -> request != null && StrUtil.isNotBlank(request.getBase64Image()))
                .toList();
        if (CollUtil.isEmpty(validRequests)) {
            return List.of();
        }
        String url = StrUtil.trimToNull(doclingProperties.getPaddleOcrUrl());
        if (url == null) {
            return validRequests.stream()
                    .map(request -> PaddleImageOcrResult.failed(request, "PaddleOCR URL is blank"))
                    .toList();
        }
        String batchUrl = resolveBatchUrl(url);
        int batchSize = resolveBatchSize();
        int batchCount = (validRequests.size() + batchSize - 1) / batchSize;
        log.info("PaddleOCR batch request start. totalCount={} batchSize={} batchCount={} url={}",
                validRequests.size(), batchSize, batchCount, batchUrl);
        List<PaddleImageOcrResult> allResults = new ArrayList<>(validRequests.size());
        try {
            for (int batchNo = 1, fromIndex = 0; fromIndex < validRequests.size(); batchNo++, fromIndex += batchSize) {
                int toIndex = Math.min(fromIndex + batchSize, validRequests.size());
                List<ImageOcrRequest> batch = validRequests.subList(fromIndex, toIndex);
                allResults.addAll(recognizeBatchPart(url, batchUrl, batch, batchNo, batchCount));
            }
            log.info("PaddleOCR batch request finished. totalCount={} success={} empty={} failed={}",
                    validRequests.size(), countByStatus(allResults, PaddleImageOcrResult.Status.SUCCESS),
                    countByStatus(allResults, PaddleImageOcrResult.Status.EMPTY),
                    countByStatus(allResults, PaddleImageOcrResult.Status.FAILED));
            return allResults;
        } catch (BatchEndpointUnavailableException e) {
            log.info("PaddleOCR batch endpoint is unavailable, fallback to single requests. url={}", batchUrl);
            return validRequests.stream()
                    .map(this::recognizeSingleResult)
                    .toList();
        }
    }

    private List<PaddleImageOcrResult> recognizeBatchPart(String url, String batchUrl, List<ImageOcrRequest> batch,
                                                          int batchNo, int batchCount) {
        log.info("PaddleOCR batch part start. batch={}/{} count={} url={}",
                batchNo, batchCount, batch.size(), batchUrl);
        try {
            List<PaddleImageOcrResult> results = postBatch(url, batch);
            log.info("PaddleOCR batch part finished. batch={}/{} count={} success={} empty={} failed={}",
                    batchNo, batchCount, batch.size(),
                    countByStatus(results, PaddleImageOcrResult.Status.SUCCESS),
                    countByStatus(results, PaddleImageOcrResult.Status.EMPTY),
                    countByStatus(results, PaddleImageOcrResult.Status.FAILED));
            return results;
        } catch (BatchEndpointUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("PaddleOCR batch part error. batch={}/{} count={} url={}: {}",
                    batchNo, batchCount, batch.size(), batchUrl, e.getMessage());
            return batch.stream()
                    .map(request -> PaddleImageOcrResult.failed(request, e.getMessage()))
                    .toList();
        }
    }

    private PaddleImageOcrResult recognizeSingleResult(ImageOcrRequest request) {
        if (!isEnabled() || request == null || StrUtil.isBlank(request.getBase64Image())) {
            return PaddleImageOcrResult.empty(request);
        }
        String url = StrUtil.trimToNull(doclingProperties.getPaddleOcrUrl());
        if (url == null) {
            return PaddleImageOcrResult.failed(request, "PaddleOCR URL is blank");
        }
        try {
            JSONObject payload = JSONUtil.createObj().set(IMAGE_BASE64_FIELD, request.getBase64Image());
            HttpResponse response = HttpRequest.post(url)
                    .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                    .body(payload.toString())
                    .timeout(resolveTimeoutMillis())
                    .execute();
            if (!response.isOk()) {
                log.warn("PaddleOCR request failed. documentId={} imageIndex={} httpStatus={} response={}",
                        request.getDocumentId(), request.getImageIndex(), response.getStatus(), StrUtil.maxLength(response.body(), 300));
                return PaddleImageOcrResult.failed(request, response.body());
            }
            String text = normalizeText(extractText(response.body()));
            return StrUtil.isBlank(text) ? PaddleImageOcrResult.empty(request) : PaddleImageOcrResult.success(request, text);
        } catch (Exception e) {
            log.warn("PaddleOCR request error. documentId={} imageIndex={}: {}",
                    request.getDocumentId(), request.getImageIndex(), e.getMessage());
            return PaddleImageOcrResult.failed(request, e.getMessage());
        }
    }

    private List<PaddleImageOcrResult> postBatch(String url, List<ImageOcrRequest> requests) {
        JSONObject payload = JSONUtil.createObj();
        JSONArray images = JSONUtil.createArray();
        Map<String, ImageOcrRequest> requestById = new LinkedHashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            ImageOcrRequest request = requests.get(i);
            String id = resolveRequestId(request, i);
            requestById.put(id, request);
            images.add(JSONUtil.createObj()
                    .set(ID_FIELD, id)
                    .set(IMAGE_BASE64_FIELD, request.getBase64Image()));
        }
        payload.set(IMAGES_FIELD, images);
        HttpResponse response = HttpRequest.post(resolveBatchUrl(url))
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(payload.toString())
                .timeout(resolveTimeoutMillis())
                .execute();
        if (isBatchEndpointUnavailable(response.getStatus())) {
            throw new BatchEndpointUnavailableException();
        }
        if (!response.isOk()) {
            log.warn("PaddleOCR batch request failed. count={} httpStatus={} response={}",
                    requests.size(), response.getStatus(), StrUtil.maxLength(response.body(), 300));
            return requests.stream()
                    .map(request -> PaddleImageOcrResult.failed(request, response.body()))
                    .toList();
        }
        return parseBatchResponse(response.body(), requestById);
    }

    private List<PaddleImageOcrResult> parseBatchResponse(String responseBody, Map<String, ImageOcrRequest> requestById) {
        JSONObject response = JSONUtil.parseObj(responseBody);
        JSONArray items = response.getJSONArray("items");
        Map<String, PaddleImageOcrResult> resultById = new LinkedHashMap<>();
        if (items != null) {
            for (Object item : items) {
                if (!(item instanceof JSONObject obj)) {
                    continue;
                }
                String id = obj.getStr(ID_FIELD);
                ImageOcrRequest request = requestById.get(id);
                if (request == null) {
                    continue;
                }
                resultById.put(id, parseBatchItem(request, obj));
            }
        }
        List<PaddleImageOcrResult> results = new ArrayList<>();
        requestById.forEach((id, request) -> results.add(resultById.getOrDefault(
                id, PaddleImageOcrResult.failed(request, "PaddleOCR batch item missing"))));
        return results;
    }

    private PaddleImageOcrResult parseBatchItem(ImageOcrRequest request, JSONObject item) {
        String status = StrUtil.trimToEmpty(item.getStr(STATUS_FIELD));
        String text = normalizeText(extractText(item));
        if (PaddleImageOcrResult.Status.FAILED.name().equalsIgnoreCase(status)) {
            return PaddleImageOcrResult.failed(request, item.getStr(ERROR_FIELD, ""));
        }
        if (StrUtil.isBlank(text)) {
            return PaddleImageOcrResult.empty(request);
        }
        return PaddleImageOcrResult.success(request, text);
    }

    private String resolveBatchUrl(String url) {
        String trimmedUrl = StrUtil.removeSuffix(url.trim(), "/");
        if (trimmedUrl.endsWith("/ocr")) {
            return trimmedUrl + BATCH_PATH_SUFFIX;
        }
        return trimmedUrl + "/ocr" + BATCH_PATH_SUFFIX;
    }

    private String resolveRequestId(ImageOcrRequest request, int position) {
        if (request.getImageIndex() != null) {
            return String.valueOf(request.getImageIndex());
        }
        return String.valueOf(position + 1);
    }

    private boolean isBatchEndpointUnavailable(int status) {
        return status == HTTP_NOT_FOUND || status == HTTP_METHOD_NOT_ALLOWED;
    }

    private long countByStatus(List<PaddleImageOcrResult> results, PaddleImageOcrResult.Status status) {
        if (CollUtil.isEmpty(results)) {
            return 0;
        }
        return results.stream()
                .filter(result -> result != null && result.getStatus() == status)
                .count();
    }

    private boolean isEnabled() {
        return doclingProperties != null && doclingProperties.isPaddleOcrEnabled();
    }

    private int resolveTimeoutMillis() {
        int timeoutMillis = doclingProperties.getPaddleOcrTimeoutMillis();
        return timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MILLIS;
    }

    private int resolveBatchSize() {
        int batchSize = doclingProperties.getPaddleOcrBatchSize();
        return batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
    }

    private String extractText(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return "";
        }
        try {
            Object json = JSONUtil.parse(responseBody);
            return extractText(json);
        } catch (Exception e) {
            return responseBody;
        }
    }

    private String extractText(Object node) {
        if (node instanceof JSONObject obj) {
            String directText = firstTextValue(obj);
            if (StrUtil.isNotBlank(directText)) {
                return directText;
            }
            List<String> values = new ArrayList<>();
            collectText(obj.get(DATA_FIELD), values);
            collectText(obj.get(RESULTS_FIELD), values);
            collectText(obj.get(RESULT_FIELD), values);
            if (CollUtil.isNotEmpty(values)) {
                return String.join("\n", values);
            }
            for (String key : obj.keySet()) {
                collectText(obj.get(key), values);
            }
            return CollUtil.isEmpty(values) ? "" : String.join("\n", values);
        }
        if (node instanceof JSONArray array) {
            List<String> values = new ArrayList<>();
            collectText(array, values);
            return CollUtil.isEmpty(values) ? "" : String.join("\n", values);
        }
        return "";
    }

    private String firstTextValue(JSONObject obj) {
        String text = obj.getStr(TEXT_FIELD);
        if (StrUtil.isNotBlank(text)) {
            return text;
        }
        text = obj.getStr(OCR_TEXT_FIELD);
        if (StrUtil.isNotBlank(text)) {
            return text;
        }
        text = obj.getStr(CONTENT_FIELD);
        if (StrUtil.isNotBlank(text)) {
            return text;
        }
        Object recTexts = obj.get(REC_TEXTS_FIELD);
        if (recTexts instanceof JSONArray array) {
            List<String> values = new ArrayList<>();
            collectText(array, values);
            if (CollUtil.isNotEmpty(values)) {
                return String.join("\n", values);
            }
        }
        return "";
    }

    private void collectText(Object node, List<String> values) {
        if (node instanceof JSONObject obj) {
            String text = firstTextValue(obj);
            if (StrUtil.isNotBlank(text)) {
                values.add(text);
                return;
            }
            for (String key : obj.keySet()) {
                collectText(obj.get(key), values);
            }
            return;
        }
        if (node instanceof JSONArray array) {
            for (Object item : array) {
                if (item instanceof CharSequence text && StrUtil.isNotBlank(text)) {
                    values.add(text.toString());
                    continue;
                }
                collectText(item, values);
            }
        }
    }

    private String normalizeText(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        return text.lines()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private static class BatchEndpointUnavailableException extends RuntimeException {
    }
}
