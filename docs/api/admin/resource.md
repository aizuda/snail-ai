# 资源 API

资源（Resource）模块提供文件资源的上传、分页查询、预览、下载及删除能力。上传的资源可关联到业务对象（如智能体头像、文档等）。

---

## 上传资源

```
POST /snail-ai/resource/upload
```

**Content-Type:** `multipart/form-data`

**表单字段：**

| 字段      | 类型     | 必填 | 说明                                      |
|-----------|----------|------|-------------------------------------------|
| `file`    | `File`   | 是   | 上传的文件                                |
| `bizType` | `string` | 否   | 业务类型（如 `avatar`、`document` 等）    |
| `bizId`   | `number` | 否   | 关联的业务对象 ID                         |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/resource/upload' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -F 'file=@/path/to/image.png' \
  -F 'bizType=avatar' \
  -F 'bizId=1'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 42,
    "storageKey": "2025/06/01/abc123.png",
    "originalName": "image.png",
    "fileSize": 204800,
    "mimeType": "image/png",
    "storageType": "LOCAL",
    "accessUrl": "/resource/42/preview",
    "bizType": "avatar",
    "bizId": 1,
    "creatorId": 10,
    "createDt": "2025-06-01 10:00:00"
  }
}
```

---

## 获取资源分页列表

```
GET /snail-ai/resource/page
```

**请求参数：**

| 参数            | 类型       | 必填 | 说明               |
|-----------------|------------|------|--------------------|
| `page`          | `number`   | 否   | 页码，默认 `1`     |
| `size`          | `number`   | 否   | 每页条数，默认 `10`|
| `bizType`       | `string`   | 否   | 业务类型过滤       |
| `bizId`         | `number`   | 否   | 业务对象 ID 过滤   |
| `originalName`  | `string`   | 否   | 文件名搜索         |
| `datetimeRange` | `string[]` | 否   | 时间范围过滤       |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/resource/page?page=1&size=10&bizType=avatar' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "data": [
      {
        "id": 42,
        "storageKey": "2025/06/01/abc123.png",
        "originalName": "image.png",
        "fileSize": 204800,
        "mimeType": "image/png",
        "storageType": "LOCAL",
        "accessUrl": "/resource/42/preview",
        "bizType": "avatar",
        "bizId": 1,
        "creatorId": 10,
        "createDt": "2025-06-01 10:00:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 42
  }
}
```

---

## 预览资源

获取资源文件的预览内容（直接返回文件二进制流）。

```
GET /snail-ai/resource/{id}/preview
```

> 返回文件原始 MIME 类型的二进制流，适合在浏览器中直接预览图片、PDF 等。

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/resource/42/preview' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -o preview.png
```

---

## 下载资源

```
GET /snail-ai/resource/{id}/download
```

> 返回二进制流，带 `Content-Disposition: attachment` 头，触发浏览器下载。

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/resource/42/download' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -o image.png
```

---

## 删除资源

```
DELETE /snail-ai/resource/{id}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/resource/42' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```

---

## 资源对象字段说明

| 字段          | 类型     | 说明                           |
|---------------|----------|--------------------------------|
| `id`          | `number` | 资源 ID                       |
| `storageKey`  | `string` | 存储路径                       |
| `originalName`| `string` | 原始文件名                     |
| `fileSize`    | `number` | 文件大小（字节）               |
| `mimeType`    | `string` | MIME 类型                      |
| `storageType` | `string` | 存储类型（如 `LOCAL`、`OSS`）  |
| `accessUrl`   | `string` | 访问 URL                       |
| `bizType`     | `string` | 业务类型                       |
| `bizId`       | `number` | 关联业务对象 ID                |
| `creatorId`   | `number` | 上传者 ID                      |
| `createDt`    | `string` | 创建时间                       |
