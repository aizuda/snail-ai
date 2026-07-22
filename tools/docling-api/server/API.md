# Docling API 对接文档

> 业务系统集成参考，面向后端开发人员。

## 服务信息

| 项目 | 值 |
|------|-----|
| 默认地址 | `http://localhost:5100` |
| API 文档 | `http://localhost:5100/docs` (Swagger) |
| 协议 | HTTP REST，JSON / FormData |
| 认证 | 可选 `X-API-Key` header |

---

## 端点

### GET /health

健康检查，可用于负载均衡探测。

**响应 200:**
```json
{
  "status": "ok",
  "version": "2.0",
  "model_loaded": true
}
```

---

### POST /convert

转换文档，支持文件上传或 URL。

**Content-Type:** `multipart/form-data`

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `file` | file | 否* | — | 上传文件（与 url 二选一） |
| `url` | string | 否* | — | 文档 URL（服务端下载） |
| `to` | string | 否 | `md` | 输出格式: `md` `json` `html` `text` |
| `ocr` | bool | 否 | `true` | 启用 OCR（扫描件必开） |
| `tables` | bool | 否 | `true` | 启用表格结构识别 |

*\* `file` 和 `url` 必须提供一个。*

---

## PDF / OCR 注意事项

DOCX/PPTX/XLSX 等 Office 文件通常只读取文档结构；PDF 会额外加载 Docling 的 PDF 版面识别、表格识别和 OCR 模型。扫描件 PDF 或图片型 PDF 必须启用 OCR，普通 PDF 也可能在版面分析阶段触发模型加载。

服务端已将 Docling OCR 固定为 RapidOCR `onnxruntime` 后端，运行环境必须安装：

```bat
python -m pip install -r requirements.txt
```

其中至少包含：

```txt
docling
rapidocr
onnxruntime
```

如果 PDF 报 `Unsupported configuration: torch.PP-OCRv6.det.small`，说明当前进程没有加载到 `onnxruntime`，Docling 自动回退到了 RapidOCR `torch` 后端。安装依赖后需要重启 Docling API。

如果 PDF 报 HuggingFace 连接超时或模型缓存不存在，说明 Docling PDF 布局/表格/OCR 模型尚未下载。联网环境下在 `tools\docling-api` 目录执行 `setup-docling-api-env.bat` 下载依赖和模型；脚本默认直连 HuggingFace，失败后会自动尝试 `https://hf-mirror.com`，也可以通过 `DOCLING_HF_ENDPOINT` 指定内网镜像。离线环境需要先在已成功解析 PDF 的机器上打包 `%USERPROFILE%\.cache\docling\models`，再解压到目标机器相同路径。

服务启动脚本会设置 `DOCLING_MODEL_DIR=%USERPROFILE%\.cache\docling\models`，服务端会将该目录传给 Docling 的 `artifacts_path`，避免转换时再次访问 HuggingFace。

---

## 响应示例

### Markdown (`to=md`, 默认)

```
Content-Type: text/markdown

# 文档标题

文档正文内容...

| 列A | 列B |
|-----|-----|
| 值1 | 值2 |

![图片](data:image/png;base64,...)
```

### JSON (`to=json`)

```json
{
  "schema": "docling",
  "body": {
    "children": [
      {
        "type": "heading",
        "text": "文档标题",
        "level": 1
      }
    ]
  }
}
```

### HTML (`to=html`)

```html
Content-Type: text/html

<h1>文档标题</h1>
<p>文档正文内容...</p>
<table>...</table>
```

### 纯文本 (`to=text`)

```
Content-Type: text/plain

文档标题
文档正文内容...
```

---

## 错误码

| 状态码 | 说明 |
|--------|------|
| 400 | 缺少 `file` 或 `url` 参数 |
| 401 | API Key 无效（启用了认证时） |
| 413 | 文件过大（Nginx/反向代理限制） |
| 422 | 文档无法解析（格式不支持/损坏） |
| 500 | 服务器内部错误 |
| 504 | 转换超时（大文档） |

---

## 对接示例

### cURL

```bash
# 上传文件
curl -F "file=@document.pdf" http://localhost:5100/convert

# URL 转换
curl -F "url=https://example.com/report.docx" -F "to=json" http://localhost:5100/convert

# 带 API Key
curl -F "file=@document.pdf" -H "X-API-Key: your-secret-key" http://localhost:5100/convert
```

### Java (HttpClient, JDK 11+)

```java
// 使用项目自带的 DoclingClient
DoclingClient client = new DoclingClient("http://localhost:5100");

// 同步
String md = client.convertFile(Path.of("document.pdf"), "md");

// 异步
client.convertFileAsync(Path.of("document.pdf"), "json")
      .thenAccept(System.out::println);
```

### Python

```python
import requests

# 文件上传
with open("document.pdf", "rb") as f:
    resp = requests.post(
        "http://localhost:5100/convert",
        files={"file": f},
        data={"to": "md"}
    )
print(resp.text)

# URL 转换
resp = requests.post(
    "http://localhost:5100/convert",
    data={"url": "https://example.com/doc.pdf", "to": "json"}
)
print(resp.json())
```

### JavaScript (Node.js)

```js
const fs = require("fs");
const FormData = require("form-data");

const form = new FormData();
form.append("file", fs.createReadStream("document.pdf"));
form.append("to", "md");

fetch("http://localhost:5100/convert", { method: "POST", body: form })
  .then(r => r.text())
  .then(console.log);
```

---

## 生产环境建议

### 1. Nginx 反向代理

```nginx
server {
    listen 80;
    server_name docling.internal;

    client_max_body_size 100M;
    proxy_read_timeout 300s;

    location / {
        proxy_pass http://127.0.0.1:5100;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 2. 启用 API Key 认证

设置环境变量后重启：

```bat
set DOCLING_API_KEY=your-random-secret-key
```

所有请求需带 `X-API-Key: your-random-secret-key` header（`/health` 和 `/docs` 除外）。

### 3. 超时建议

| 文档类型 | 建议超时 |
|----------|----------|
| 10页以内 PDF | 30s |
| 10-50页 PDF | 120s |
| 50页以上 PDF | 300s |
| DOCX/PPTX | 30s |
| 图片 OCR | 60s |

### 4. 并发建议

docling 是 CPU 密集型，单机建议并发 ≤2。高并发场景建议：
- 部署多实例 + 负载均衡
- 引入任务队列（Celery/Redis），异步处理
