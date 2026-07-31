# Snail AI RAG 知识库 — Docling + OCR 集成设计深度分析

## 1. 概述

Snail AI 的 RAG（检索增强生成）知识库在传统文档解析之上引入了两套外部 AI 服务作为增强能力：

| 服务 | 作用 | 技术栈 |
|------|------|--------|
| **Docling Serve** | 高精度文档结构化解析（布局识别、表格、图片提取） | Python + IBM Docling + FastAPI |
| **PaddleOCR** | 图片文字识别（OCR），补充 Docling 未提取的文字 | Python + PaddleOCR 3.7 + FastAPI |

二者形成"解析 → OCR 补充"的流水线，整体设计遵循"增强而非依赖"原则：任何外部服务异常时自动降级，不阻断主流程。

---

## 2. 前期准备工作

### 2.1 环境依赖

**Python 服务（两个独立进程）**：

```bash
# Docling Serve（端口 5100）
cd tools/docling-api/server
pip install docling docling-core fastapi uvicorn
python main.py
# 或: uvicorn main:app --host 0.0.0.0 --port 5100

# PaddleOCR API（端口 8866）
cd tools/paddle-ocr-api/server
pip install paddleocr==3.7.0 paddlex==3.7.2 fastapi uvicorn
python main.py
# 或: uvicorn main:app --host 0.0.0.0 --port 8866
```

**Java 端 Maven 依赖**（在 `snail-ai-feature-rag/pom.xml` 中）：

- `hutool-http` — Docling/PaddleOCR HTTP 客户端
- `hutool-json` — JSON 响应解析
- `pdfbox` + `poi-ooxml` — 内置解析器（降级用）
- `spring-ai-milvus-store` / `spring-ai-pgvector-store` — 向量存储
- `spring-ai-client-chat` — 视觉模型 OCR 兜底

### 2.2 数据库准备

SQL 脚本位于 `script/sql/snail_ai_schema.sql`，涉及三张核心表：

```sql
-- sai_rag_document: 文档表（含 Docling 扩展字段）
page_count, element_count, table_count, image_count,
parse_time, md_content LONGTEXT, doc_metadata LONGTEXT

-- sai_rag_document_image: 图片关系表（新增）
id, rag_id, document_id, resource_id, chunk_id,
image_index, image_url, caption, figure_no, figure_title,
section_title, source_page, document_name, ocr_text

-- sai_rag_chunk: 切片表
content_hash 字段用于去重
```

### 2.3 环境变量

**Docling Serve 环境变量**：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DOCLING_HOST` | `0.0.0.0` | 监听地址 |
| `DOCLING_PORT` | `5100` | 监听端口 |
| `DOCLING_TASK_TTL` | `3600` | 异步任务过期时间（秒） |
| `DOCLING_MAX_CONCURRENCY` | `3` | 最大并发转换数 |
| `DOCLING_API_KEY` | 空 | API 认证密钥（可选） |

**PaddleOCR 环境变量**：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `PADDLE_OCR_LANG` | `ch` | OCR 语言 |
| `PADDLE_OCR_DEVICE` | 自动 | 推理设备（cpu/gpu） |
| `PADDLE_OCR_CPU_THREADS` | 自动 | CPU 线程数 |
| `PADDLE_OCR_MAX_CONCURRENCY` | `1` | 最大并发 OCR 数 |
| `PADDLE_OCR_QUEUE_TIMEOUT_SECONDS` | `300` | 队列等待超时 |
| `PADDLE_OCR_API_KEY` | 空 | API 认证密钥（可选） |
| `PADDLE_OCR_MAX_IMAGE_MB` | `20` | 单张图片最大 MB |

---

## 3. 配置体系

### 3.1 应用级配置（application.yml）

```yaml
snail-ai:
  rag:
    docling:
      enabled: true                      # Docling 总开关
      url: http://127.0.0.1:5100        # Docling Serve 地址
      timeout-seconds: 300               # 单任务整体超时
      concurrency: 3                     # 最大并发数（Semaphore）
      health-timeout-millis: 3000        # 健康检查超时
      poll-interval-millis: 3000         # 状态轮询间隔
      status-timeout-millis: 10000       # 状态查询 HTTP 超时
      result-timeout-millis: 30000       # 结果拉取 HTTP 超时
      max-image-count: 100               # 单文档最大保存图片数
      max-image-bytes: 10485760          # 单张图片最大字节(10MB)
      paddle-ocr-enabled: true           # PaddleOCR 开关
      paddle-ocr-url: http://127.0.0.1:8866/ocr
      paddle-ocr-timeout-millis: 180000  # OCR 超时(3分钟)
      paddle-ocr-batch-size: 5           # 批处理大小
      vision-ocr-fallback-enabled: false # 视觉模型兜底开关
```

### 3.2 知识库级配置（sai_rag.config JSON）

每个知识库在 `config` 字段存储 JSON，覆盖全局配置：

```json
{
  "parseParams": {
    "engine": "docling",
    "docling": {
      "doOcr": true,
      "doTableStructure": true,
      "imageExportMode": "embedded",
      "ocrLang": ["en", "zh"],
      "pdfBackend": "dlparse_v4",
      "saveImages": true,
      "maxImageCount": 50,
      "maxImageBytes": 5242880
    }
  },
  "chunkParams": {
    "imageOcr": true
  }
}
```

### 3.3 配置优先级

```
知识库级 DoclingParams  >  应用级 DoclingProperties  >  代码默认值
```

- `engine` 决定选哪个解析引擎（`default` / `docling`）
- `chunkParams.imageOcr` 决定是否对 Docling 提取的图片执行 OCR
- `parseParams.docling.*` 映射为 Docling Serve HTTP 请求参数
- 图片保存限制由 `DoclingImageSavePolicy` 合并两级配置

---

## 4. 架构设计

### 4.1 模块分层

```
snail-ai-feature-rag/
├── docling/                          ← Docling HTTP 客户端层
│   ├── DoclingClient.java            异步三步协议（submit→poll→fetch）
│   ├── DoclingProperties.java        @ConfigurationProperties
│   ├── DoclingConvertOptions.java    转换参数 DTO
│   ├── DoclingParseResult.java       解析结果 DTO（elements + images）
│   ├── DoclingResponseParser.java    JSON $ref 展开 + Markdown 重建
│   └── DoclingMarkdownSanitizer.java 清理 base64 图片/多余空行
├── pipeline/                         ← 流水线编排层
│   ├── DocumentPipeline.java         总调度器（CAS→解析→OCR→切片→双写）
│   ├── DoclingImageOcrApplier.java   OCR 文本合并到解析结果
│   ├── PaddleImageOcrHandler.java    PaddleOCR HTTP 调用（单张+批量）
│   ├── DoclingImageOcrService.java   视觉模型 OCR 兜底
│   └── DoclingImageSavePolicy.java   图片保存策略（数量/大小限制）
├── strategy/parser/                  ← 解析器策略层
│   ├── DocumentParser.java           解析器接口
│   ├── DocumentParserFactory.java    工厂（按 engine + fileType 选择）
│   ├── DoclingParser.java            Docling 策略实现
│   └── DocumentParseResult.java      解析结果 DTO
└── enums/
    └── DocumentParseEngineEnum.java  DEFAULT / DOCLING
```

### 4.2 类关系与数据流

```
DocumentProcessingTask (@Scheduled 10s)
  └─▶ DocumentPipeline.processDocument()
        ├─▶ CAS: PENDING → PROCESSING
        ├─▶ parseKnowledgeConfig() → RagConfigDO
        ├─▶ DocumentParserFactory.getParser(request)
        │     ├─ engine=DEFAULT → 内置解析器（PDFBox/POI）
        │     └─ engine=DOCLING → DoclingParser
        │           └─▶ DoclingClient.convert(file, options)
        │                 ├─ POST /v1/convert/file/async → task_id
        │                 ├─ poll GET /v1/status/poll/{task_id}
        │                 └─ GET /v1/result/{task_id} → JSON
        │                       └─▶ DoclingResponseParser.parse()
        │                             ├─ 展开 $ref 引用（texts/pictures/tables/groups）
        │                             ├─ 类型映射（label → type）
        │                             └─ 提取图片 base64 + caption + OCR
        ├─▶ DoclingImageOcrApplier.apply()
        │     ├─ 检查 chunkParams.imageOcr
        │     ├─ 复用 Docling 已提取的文字（annotations）
        │     ├─ PaddleOCR 批量识别（主）
        │     └─ 视觉模型兜底（可选）
        ├─▶ cleanExistingData() — 清理旧切片+向量+图片
        ├─▶ documentChunkingService.chunk() — 文本切片
        ├─▶ dualWrite() — 向量库 + 搜索引擎双写
        ├─▶ saveDoclingImages() — 图片保存到资源库
        └─▶ status → SUCCESS
```

### 4.3 Docling Serve 异步三步协议

Docling 处理大文件耗时较长，采用异步模型：

```
步骤1: POST /v1/convert/file/async
  → multipart/form-data: files + to_formats + do_ocr + do_table_structure + ...
  ← { "task_id": "abc123" }

步骤2: GET /v1/status/poll/{task_id}  （每 3 秒轮询）
  ← { "task_status": "pending" } → 继续轮询
  ← { "task_status": "success" } → 进入步骤3

步骤3: GET /v1/result/{task_id}
  ← { "document": { "json_content": {...}, "md_content": "..." } }
```

每次同时请求 `md` 和 `json` 两种格式：
- **Markdown** → 清理 base64 后直接用于切片和向量化
- **JSON** → 提取页数、表格、图片、元数据等统计信息

### 4.4 JSON $ref 引用展开机制

Docling Serve 返回的 JSON 使用 `$ref` 分离结构（阅读顺序）与内容：

```json
{
  "body": {
    "children": [
      { "$ref": "#/texts/0" },
      { "$ref": "#/pictures/0" },
      { "$ref": "#/tables/0" }
    ]
  },
  "texts": [{ "text": "...", "label": "title" }],
  "pictures": [{ "image": { "uri": "data:image/png;base64,..." } }],
  "tables": [{ "data": { "table_cells": [...] } }]
}
```

`DoclingResponseParser.resolveChildren()` 递归展开：
1. 遍历 `body.children`，解析 `$ref` 类别和索引
2. `#/groups/N` → 递归展开 `group.children`
3. `#/texts/N` → 添加文本元素（label → type 映射）
4. `#/tables/N` → 构建 Markdown 表格
5. `#/pictures/N` → 提取 base64 + 前瞻查找图注

### 4.5 标签类型映射

| Docling label | 映射 type | 说明 |
|---------------|-----------|------|
| `title` | `title` | 文档标题 |
| `section_header` | `title` | 章节标题 |
| `list_item` | `list` | 列表项 |
| `formula` / `equation` | `formula` | 公式 |
| 其他 | `paragraph` | 普通段落 |

### 4.6 Markdown 重建规则

`DoclingResponseParser.toCleanMarkdown()` 从结构化元素重建 Markdown：

| 元素类型 | 输出格式 |
|---------|---------|
| `title` | `#` × level + 文本（如 `## 第一章`） |
| `paragraph` / `list` | 纯文本 + 双换行 |
| `table` | 解析时已转为 Markdown 表格格式 |
| `picture` | `[图片说明: caption + OCR]` 注入 chunk |
| `formula` | LaTeX 原文保留 |

最终经 `DoclingMarkdownSanitizer` 清理：
- 移除 base64 内嵌图片 → `[image]` 占位
- 压缩连续 3+ 空行为 2 个

---

## 5. OCR 图片文字识别流程

### 5.1 触发条件

OCR 流程在 `DocumentPipeline.processDocument()` 中解析完成之后、切片之前执行：

```java
// 在 DocumentPipeline 中
DocumentParseResult parseResult = parseDocument(document, config);
doclingImageOcrApplier.apply(config, document, parseResult);  // ← OCR 这里
String content = parseResult.getContent();
```

必须同时满足：
1. 知识库配置 `chunkParams.imageOcr = true`
2. `parseResult.doclingResult != null`（走了 Docling 解析）
3. `doclingResult.images` 非空

### 5.2 OCR 处理优先级（三级策略）

```
第一级：Docling 自带文字（annotations）
  → 图片元素已有有效文字 → 直接复用，不调 PaddleOCR

第二级：PaddleOCR 批量识别（主通道）
  → 收集无文字的图片 → 批量 POST /ocr/batch
  → 批次大小由 paddleOcrBatchSize 控制（默认 5）
  → 如果 batch 端点不可用（404/405），自动降级为逐张 /ocr

第三级：视觉模型兜底（可选，默认关闭）
  → vision-ocr-fallback-enabled=true 时启用
  → 使用系统默认视觉模型（VisionModelConfigResolver）
  → PaddleOCR 无结果时调用，遇到配额耗尽自动停止
```

### 5.3 OCR 结果合并

OCR 文字通过以下方式合并回解析结果：

1. **已有对应 picture 元素** → 合并 `ocrText` 到 `element.text` 末尾
2. **无对应元素** → 追加新的 `picture` 类型元素到 elements 列表
3. 合并后重新生成干净的 Markdown（`toCleanMarkdown()`）
4. 更新 `parseResult.content` 和 `parseResult.markdownContent`

### 5.4 PaddleOCR HTTP 调用细节

**单张 OCR**：`POST /ocr` → `{"imageBase64": "..."}` ← `{"text": "...", "results": [...], "elapsedMs": 123}`

**批量 OCR**：`POST /ocr/batch` → `{"images": [{"id":"1","imageBase64":"..."}]}` ← `{"status":"SUCCESS","items":[...]}`

客户端（`PaddleImageOcrHandler`）特性：
- 优先尝试批量端点，失败时自动降级为逐张调用
- 响应解析兼容多种 JSON 格式（`text` / `ocrText` / `content` / `rec_texts` 字段）
- 每批之间有独立异常处理，一批失败不影响其他批次
- 结果状态分三类：`SUCCESS`（有文字）、`EMPTY`（无文字）、`FAILED`（调用失败）

---

## 6. 图片保存流程

### 6.1 保存条件

- 知识库配置 `parseParams.docling.saveImages` 不为 `false`（默认保存）
- Docling 解析结果中存在有效 base64 图片

### 6.2 保存限制（两级合并）

| 限制项 | 全局默认 | 知识库级覆盖路径 |
|--------|---------|-----------------|
| `maxImageCount` | 100 | `parseParams.docling.maxImageCount` |
| `maxImageBytes` | 10 MB | `parseParams.docling.maxImageBytes` |

超限时记录 warn 日志并跳过，不阻断流程。

### 6.3 保存步骤

```
遍历 doclingResult.images
  → Base64 解码
  → 大小校验
  → resourceService.upload() → ResourcePO（bizType=DOCUMENT_IMAGE）
  → ragDocumentImageMapper.insert() → 记录图片关系
  → DocumentImageChunkMatcher 匹配关联切片
```

### 6.4 图片清理

- 文档删除或覆盖上传（OVERWRITE）时同步清理
- 先删资源库文件 → 再删 `sai_rag_document_image` 表记录
- 资源删除失败只记日志，不阻断重处理

---

## 7. 降级保护设计

Docling 是增强能力，**绝不阻断主流程**。降级链路：

```
DocumentPipeline.parseDocument()
  ├─ 知识库 engine=docling → DoclingParser.parse()
  │   ├─ 成功 → 返回含 doclingResult 的解析结果
  │   └─ 任何异常 → log.warn + parseWithFallback()
  │       └─ 重新加载资源流 → 内置解析器（PDFBox/POI/纯文本）
  └─ 知识库 engine=default → 内置解析器
```

降级触发条件：
- Docling Serve 健康检查失败（`/health` 非 200）
- `snail-ai.rag.docling.enabled=false`
- 异步任务返回 `failure` 状态
- 轮询超时
- 响应解析失败
- 任何网络异常

降级时仅记录 `warn` 日志，不影响文档状态流转。

---

## 8. 并发控制

### 8.1 Java 端（DoclingClient）

```java
// 初始化
parseSemaphore = new Semaphore(Math.max(1, properties.getConcurrency())); // 默认 3

// 每次 convert() 调用
parseSemaphore.acquire();
try {
    // submit → poll → fetch
} finally {
    parseSemaphore.release();
}
```

### 8.2 Python 端

- **Docling Serve**: `threading.Semaphore(DOCLING_MAX_CONCURRENCY)` — 默认 3
- **PaddleOCR API**: `threading.Semaphore(MAX_CONCURRENT_OCR)` — 默认 1，队列超时 300 秒

### 8.3 异步处理调度

```java
// DocumentProcessingTask
@Scheduled(fixedDelay = 10000)  // 每 10 秒扫描
public void process() {
    // 查询 status=PENDING 的文档
    // 逐个调用 DocumentPipeline.processDocument()
}
```

每个文档通过 CAS（`PENDING → PROCESSING`）抢占，保证单实例内同一文档只被处理一次。

---

## 9. 支持的文件类型

| 格式 | Docling | 内置解析器 | 说明 |
|------|---------|-----------|------|
| PDF | ✅ | ✅ | Docling 支持 OCR、表格、布局 |
| DOCX | ✅ | ✅ | Word 文档 |
| XLSX/XLS | ✅ | ✅ | Excel 表格 |
| PPTX | ✅ | ✅ | PowerPoint |
| HTML | ✅ | ✅ | 网页 |
| MD | ✅ | ✅ | Markdown |
| TXT | ❌ | ✅ | 纯文本 |
| CSV | ❌ | ✅ | CSV 数据 |

DoclingParser 仅当 `enabled=true` 且知识库 `engine=docling` 且文件类型在支持列表中时生效。

---

## 10. 完整数据流时序

```
用户上传文档
  → RagDocumentController.upload()
  → RagDocumentService: 去重 → 资源上传 → 写入 DB (status=PENDING)

异步处理（DocumentProcessingTask @Scheduled 10s）
  → CAS: PENDING → PROCESSING
  → 加载资源流
  → parseKnowledgeConfig() → RagConfigDO

  ┌─ 解析阶段 ─────────────────────────────────────
  │ DocumentParserFactory.getParser(request)
  │   ├─ engine=DOCLING → DoclingParser
  │   │   └─ DoclingClient: 三步协议 → DoclingParseResult
  │   └─ engine=DEFAULT → 内置解析器
  │ 异常 → 自动降级到内置解析器
  └────────────────────────────────────────────────

  ┌─ OCR 阶段 ─────────────────────────────────────
  │ DoclingImageOcrApplier.apply()
  │   ├─ 复用 Docling 已提取文字
  │   ├─ PaddleOCR 批量识别
  │   └─ 视觉模型兜底（可选）
  │ → 合并 OCR 文字 → 重建 Markdown
  └────────────────────────────────────────────────

  → cleanExistingData() — 清理旧切片+向量+图片
  → documentChunkingService.chunk() — 文本切片
  → dualWrite()
  │   ├─ 向量库（Milvus/PGVector）：batch embedding（10条/批）
  │   │   └─ contentHash 去重：复用已有向量
  │   └─ 搜索引擎（可选）：全文索引写入
  → saveDoclingImages() — 图片保存到资源库
  → 更新文档状态: status=SUCCESS, chunkCount, 统计信息
```

---

## 11. 关键设计决策总结

| 决策 | 说明 |
|------|------|
| **增强而非依赖** | Docling 异常时自动降级到内置解析器，不影响知识库导入 |
| **异步三步协议** | 适应大文件长时解析，通过 poll 解耦提交和结果获取 |
| **双格式请求** | MD 用于切片向量化，JSON 用于统计和图片提取 |
| **$ref 引用展开** | 递归解析 Docling 的结构化 JSON，保留阅读顺序 |
| **OCR 三级策略** | Docling 自带文字 > PaddleOCR > 视觉模型兜底 |
| **图片不进 chunk** | 图片文本通过 OCR 注入 chunk 可检索，图片本身单独保存到资源库 |
| **两级配置合并** | 全局 application.yml + 知识库级 config JSON，后者覆盖前者 |
| **chunk 级去重** | 通过 contentHash 复用相同内容的已有向量，节省 embedding 成本 |
| **双写架构** | 向量库（语义搜索）+ 搜索引擎（关键词搜索），互不阻塞 |
| **并发隔离** | Java 端 Semaphore + Python 端 Semaphore 双层控制 |
