# 技能 API

技能（Skill）模块提供技能包的创建、上传、下载、管理以及在线文件编辑能力。技能以 zip 包形式存储，可绑定到智能体以扩展其能力。

---

## 获取技能分页列表

```
GET /snail-ai/skill/page
```

**请求参数：**

| 参数      | 类型     | 必填 | 说明           |
|-----------|----------|------|----------------|
| `page`    | `number` | 否   | 页码，默认 `1` |
| `size`    | `number` | 否   | 每页条数       |
| `keyword` | `string` | 否   | 关键词搜索     |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/skill/page?page=1&size=10' \
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
        "id": 1,
        "name": "天气查询技能",
        "description": "通过 API 查询实时天气信息",
        "fileName": "weather-skill.zip",
        "fileSize": 10240,
        "createDt": "2025-03-01 10:00:00",
        "updateDt": "2025-05-15 14:30:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 5
  }
}
```

---

## 获取技能详情

```
GET /snail-ai/skill/{id}
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/skill/1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "天气查询技能",
    "description": "通过 API 查询实时天气信息",
    "fileName": "weather-skill.zip",
    "fileSize": 10240,
    "createDt": "2025-03-01 10:00:00",
    "updateDt": "2025-05-15 14:30:00"
  }
}
```

---

## 获取全量技能列表

获取所有技能（不分页），用于智能体配置时的下拉选择。

```
GET /snail-ai/skill/list
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/skill/list' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    { "id": 1, "name": "天气查询技能", "description": "查询实时天气" },
    { "id": 2, "name": "代码执行技能", "description": "执行 Python 代码" }
  ]
}
```

---

## 在线创建技能

仅创建技能元数据（DB 记录），后续通过文件管理接口编辑技能文件。

```
POST /snail-ai/skill
```

**请求体：**

| 字段          | 类型     | 必填 | 说明     |
|---------------|----------|------|----------|
| `name`        | `string` | 是   | 技能名称 |
| `description` | `string` | 否   | 描述     |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/skill' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "name": "数据分析技能",
    "description": "提供数据统计与可视化能力"
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 6,
    "name": "数据分析技能",
    "description": "提供数据统计与可视化能力",
    "fileName": "",
    "createDt": "2025-06-01 10:00:00"
  }
}
```

---

## 更新技能元数据

```
PUT /snail-ai/skill/{id}
```

**请求体：**

| 字段          | 类型     | 必填 | 说明     |
|---------------|----------|------|----------|
| `name`        | `string` | 否   | 技能名称 |
| `description` | `string` | 否   | 描述     |

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/skill/6' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "name": "数据分析技能 v2",
    "description": "增强版数据统计与可视化能力"
  }'
```

---

## 上传技能 zip 包

上传技能 zip 包并自动解析元数据。

```
POST /snail-ai/skill/upload
```

**Content-Type:** `multipart/form-data`

**表单字段：**

| 字段   | 类型   | 必填 | 说明              |
|--------|--------|------|-------------------|
| `file` | `File` | 是   | 技能 zip 压缩包   |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/skill/upload' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -F 'file=@/path/to/weather-skill.zip'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 7,
    "name": "weather-skill",
    "description": "天气查询技能",
    "fileName": "weather-skill.zip",
    "fileSize": 10240,
    "createDt": "2025-06-01 10:00:00"
  }
}
```

---

## 下载技能 zip 包

```
GET /snail-ai/skill/{id}/download
```

> 返回二进制流（`application/zip`），需带 `Snail-Ai-Auth` 头。

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/skill/1/download' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -o weather-skill.zip
```

---

## 删除技能

```
DELETE /snail-ai/skill/{id}
```

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/skill/1' \
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

## 文件管理（在线编辑）

### 获取技能文件树

```
GET /snail-ai/skill/{skillId}/files
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/skill/1/files' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "name": "weather-skill",
    "type": "directory",
    "children": [
      {
        "name": "index.js",
        "type": "file",
        "size": 2048
      },
      {
        "name": "config.json",
        "type": "file",
        "size": 256
      },
      {
        "name": "lib",
        "type": "directory",
        "children": [
          {
            "name": "weather-api.js",
            "type": "file",
            "size": 1024
          }
        ]
      }
    ]
  }
}
```

---

### 获取文件内容

```
GET /snail-ai/skill/{skillId}/files/content
```

**请求参数：**

| 参数   | 类型     | 必填 | 说明                       |
|--------|----------|------|----------------------------|
| `path` | `string` | 是   | 文件路径（如 `index.js`）  |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/skill/1/files/content?path=index.js' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "content": "module.exports = {\n  name: 'weather-skill',\n  ...\n}",
    "encoding": "utf-8",
    "size": 2048
  }
}
```

---

### 保存文件内容

```
PUT /snail-ai/skill/{skillId}/files/content
```

**请求参数：**

| 参数   | 类型     | 必填 | 说明     |
|--------|----------|------|----------|
| `path` | `string` | 是   | 文件路径 |

**请求体：**

| 字段       | 类型     | 必填 | 说明                |
|------------|----------|------|---------------------|
| `content`  | `string` | 是   | 文件内容            |
| `encoding` | `string` | 否   | 编码，默认 `utf-8`  |

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/skill/1/files/content?path=index.js' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "content": "module.exports = {\n  name: \"weather-skill\",\n  version: \"2.0\"\n}",
    "encoding": "utf-8"
  }'
```

---

### 新建文件/目录

```
POST /snail-ai/skill/{skillId}/files
```

**请求体：**

| 字段   | 类型     | 必填 | 说明                                 |
|--------|----------|------|--------------------------------------|
| `path` | `string` | 是   | 文件/目录路径（如 `lib/utils.js`）   |
| `type` | `string` | 是   | 类型：`file` 或 `directory`          |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/skill/1/files' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "path": "lib/utils.js",
    "type": "file"
  }'
```

---

### 删除文件/目录

```
DELETE /snail-ai/skill/{skillId}/files
```

**请求参数：**

| 参数   | 类型     | 必填 | 说明     |
|--------|----------|------|----------|
| `path` | `string` | 是   | 文件路径 |

**curl 示例：**

```bash
curl -X DELETE 'http://localhost:8900/snail-ai/skill/1/files?path=lib/utils.js' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 重命名文件/目录

```
PUT /snail-ai/skill/{skillId}/files/rename
```

**请求体：**

| 字段      | 类型     | 必填 | 说明       |
|-----------|----------|------|------------|
| `oldPath` | `string` | 是   | 原路径     |
| `newPath` | `string` | 是   | 新路径     |

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/skill/1/files/rename' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "oldPath": "lib/utils.js",
    "newPath": "lib/helpers.js"
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```
