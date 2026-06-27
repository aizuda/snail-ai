# 对话接口

对话接口提供与 Snail AI 智能体进行流式对话的能力，采用 SSE（Server-Sent Events）方式实时推送回答内容，支持多轮对话。

---

## 智能体流式对话

```
POST /snail-ai/openapi/v1/agent/chat
```

通过 SSE 流式方式与指定智能体进行对话。支持思考过程展示、多轮上下文、MCP 工具调用、RAG 知识库检索等完整的 Agent 能力。

### 请求体

| 字段                    | 类型       | 必填 | 说明                                            |
|-------------------------|------------|------|-------------------------------------------------|
| `agentId`               | `number`   | 是   | 智能体 ID                                       |
| `openId`                | `string`   | 是   | 外部用户标识（第三方系统的用户 ID）             |
| `content`               | `string`   | 是   | 用户消息内容                                    |
| `conversationId`        | `string`   | 否   | 对话 ID，多轮对话时传入以保持上下文             |
| `disabledMcpServerIds`  | `number[]` | 否   | 本次对话禁用的 MCP 服务 ID 列表                 |
| `disabledSkillIds`      | `number[]` | 否   | 本次对话禁用的技能 ID 列表                      |

### curl 示例

**新建对话：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/openapi/v1/agent/chat' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-App-Id: <your-app-id>' \
  -H 'Snail-Ai-Token: <your-app-token>' \
  -d '{
    "agentId": 1,
    "openId": "external-user-001",
    "content": "帮我分析一下上季度的销售数据"
  }' \
  --no-buffer
```

**多轮对话（传入 conversationId）：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/openapi/v1/agent/chat' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-App-Id: <your-app-id>' \
  -H 'Snail-Ai-Token: <your-app-token>' \
  -d '{
    "agentId": 1,
    "openId": "external-user-001",
    "content": "能再详细说明一下华东区域的数据吗？",
    "conversationId": "conv-abc123"
  }' \
  --no-buffer
```

**禁用特定 MCP/技能：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/openapi/v1/agent/chat' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-App-Id: <your-app-id>' \
  -H 'Snail-Ai-Token: <your-app-token>' \
  -d '{
    "agentId": 1,
    "openId": "external-user-001",
    "content": "查询今天的天气",
    "disabledMcpServerIds": [3],
    "disabledSkillIds": [2]
  }' \
  --no-buffer
```

### 响应格式

响应为 NDJSON（Newline Delimited JSON）流，每行是一个独立的 JSON 对象，通过 `type` 字段区分事件类型。

**流式输出示例：**

```json
{"type":"thinking","content":"用户需要分析销售数据，我需要先检索相关的知识库文档..."}
{"type":"thinking","content":"已找到上季度销售报告，开始生成分析结果..."}
{"type":"text","content":"根据"}
{"type":"text","content":"上季度的销售数据"}
{"type":"text","content":"分析如下："}
{"type":"text","content":"\n\n## 整体概览\n\n"}
{"type":"text","content":"- 总销售额：1,250 万元，同比增长 15.3%\n"}
{"type":"text","content":"- 订单量：8,560 笔，环比增长 8.2%\n"}
{"type":"text","content":"- 客单价：1,460 元，基本持平\n\n"}
{"type":"text","content":"## 区域分布\n\n华东区域贡献了 42% 的销售额..."}
```

### 事件类型

| type       | 说明                                                  |
|------------|-------------------------------------------------------|
| `thinking` | 智能体的思考/推理过程，可选择性展示给用户             |
| `text`     | 正式回答内容，客户端需将多个 text 事件拼接后显示      |

### 客户端处理示例

**JavaScript（浏览器/Node.js）：**

```javascript
async function chatWithAgent(agentId, openId, content, conversationId) {
  const response = await fetch('http://localhost:8900/snail-ai/openapi/v1/agent/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Snail-Ai-App-Id': 'your-app-id',
      'Snail-Ai-Token': 'your-app-token'
    },
    body: JSON.stringify({ agentId, openId, content, conversationId })
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let fullText = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
      if (!line.trim()) continue;
      try {
        const event = JSON.parse(line);
        if (event.type === 'text') {
          fullText += event.content;
          // 实时更新 UI 显示
          updateUI(fullText);
        } else if (event.type === 'thinking') {
          // 可选：展示思考过程
          showThinking(event.content);
        }
      } catch (e) {
        // 非 JSON 行，直接作为文本处理
        fullText += line;
      }
    }
  }

  return fullText;
}
```

**Python：**

```python
import requests
import json

def chat_with_agent(agent_id, open_id, content, conversation_id=None):
    url = "http://localhost:8900/snail-ai/openapi/v1/agent/chat"
    headers = {
        "Content-Type": "application/json",
        "Snail-Ai-App-Id": "your-app-id",
        "Snail-Ai-Token": "your-app-token"
    }
    payload = {
        "agentId": agent_id,
        "openId": open_id,
        "content": content
    }
    if conversation_id:
        payload["conversationId"] = conversation_id

    response = requests.post(url, json=payload, headers=headers, stream=True)
    full_text = ""

    for line in response.iter_lines(decode_unicode=True):
        if not line or not line.strip():
            continue
        try:
            event = json.loads(line)
            if event["type"] == "text":
                full_text += event["content"]
                print(event["content"], end="", flush=True)
            elif event["type"] == "thinking":
                # 可选处理思考过程
                pass
        except json.JSONDecodeError:
            full_text += line

    print()  # 换行
    return full_text
```

---

## openId 说明

`openId` 是外部系统中的用户标识，用于区分不同的外部用户。Snail AI 会根据 `openId` 维护独立的对话上下文和记忆。

| 场景           | openId 示例              |
|----------------|--------------------------|
| 飞书集成       | 飞书用户 ID              |
| 钉钉集成       | 钉钉用户 ID              |
| 企业微信       | 企业微信用户 ID          |
| 自定义前端     | 自定义用户标识           |
| 匿名用户       | 会话级 UUID              |

---

## conversationId 说明

- **首次对话**：不传 `conversationId`，服务端会自动创建新对话并在后续响应中返回
- **多轮对话**：传入已有的 `conversationId`，智能体会基于历史上下文继续对话
- **新话题**：需要开始新话题时，不传或传入新的 `conversationId`

---

## 错误处理

**智能体不存在：**

```json
{
  "code": 0,
  "msg": "智能体不存在或已被禁用",
  "data": null
}
```

**参数缺失：**

```json
{
  "code": 0,
  "msg": "agentId 和 content 为必填参数",
  "data": null
}
```

**流式过程中出错：**

如果在流式输出过程中发生错误，连接会被中断。客户端应实现断线检测和重试机制。
