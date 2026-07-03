# PaddleOCR API 对接文档

## 服务信息

| 项目 | 值 |
|------|----|
| 默认地址 | `http://127.0.0.1:8866` |
| API 文档 | `http://127.0.0.1:8866/docs` |
| 协议 | HTTP REST / JSON |
| 认证 | 可选 `X-API-Key` header |

## GET /health

健康检查。

响应示例：

```json
{
  "status": "ok",
  "version": "1.0",
  "model_loaded": false,
  "lang": "ch",
  "maxConcurrency": 1,
  "queueTimeoutSeconds": 300,
  "device": null,
  "cpuThreads": null,
  "useTextlineOrientation": true,
  "textRecognitionBatchSize": null,
  "textlineOrientationBatchSize": null
}
```

## POST /ocr

识别 Base64 图片。

请求：

```json
{
  "imageBase64": "iVBORw0KGgo...",
  "fileName": "image.png"
}
```

响应：

```json
{
  "text": "识别出的文字",
  "results": ["识别出的文字"],
  "elapsedMs": 1234
}
```

Snail AI 后端只依赖 `text` 字段；`results` 用于调试。

## POST /ocr/batch

Batch OCR for multiple Base64 images. The response keeps the same order as the request. A failed image is returned as a failed item and does not fail the whole request. Valid images in the request are sent to PaddleOCR in one `predict([...])` call.

Request:
```json
{
  "images": [
    {
      "id": "1",
      "imageBase64": "iVBORw0KGgo...",
      "fileName": "image-1.png"
    },
    {
      "id": "2",
      "imageBase64": "iVBORw0KGgo...",
      "fileName": "image-2.png"
    }
  ]
}
```

Response:
```json
{
  "status": "SUCCESS",
  "items": [
    {
      "id": "1",
      "status": "SUCCESS",
      "text": "recognized text",
      "results": ["recognized text"],
      "elapsedMs": 1234,
      "error": ""
    },
    {
      "id": "2",
      "status": "EMPTY",
      "text": "",
      "results": [],
      "elapsedMs": 980,
      "error": ""
    }
  ]
}
```

Item status values:

| Status | Description |
|--------|-------------|
| `SUCCESS` | The image was recognized and contains text |
| `EMPTY` | The image was processed but no text was recognized |
| `FAILED` | The image failed; see `error` |

Top-level `status` is `SUCCESS` when the batch request finishes with at least one non-failed item. It is `FAILED` only when all images fail.

## cURL 示例

```bash
curl -X POST http://127.0.0.1:8866/ocr ^
  -H "Content-Type: application/json" ^
  -d "{\"imageBase64\":\"BASE64_IMAGE\"}"
```

启用 API Key 后：

```bash
curl -X POST http://127.0.0.1:8866/ocr ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: your-secret" ^
  -d "{\"imageBase64\":\"BASE64_IMAGE\"}"
```

Batch OCR:

```bash
curl -X POST http://127.0.0.1:8866/ocr/batch ^
  -H "Content-Type: application/json" ^
  -d "{\"images\":[{\"id\":\"1\",\"imageBase64\":\"BASE64_IMAGE\"}]}"
```

## 错误码

| 状态码 | 说明 |
|--------|------|
| 400 | 图片为空或 Base64 非法 |
| 401 | API Key 无效 |
| 413 | 图片超过大小限制 |
| 503 | OCR 服务忙，超过排队等待时间 |
| 500 | OCR 识别失败 |
