# 对话内联上传图片设计方案

## 背景

当前智能体对话接口是纯文本链路：前端通过 `/agent/{id}/chat` 发送 JSON 请求并接收 SSE 流，服务端组装 `ChatDispatchRequest` 后通过 gRPC 分发给 agent client，client 侧 `DefaultPromptFactory` 构造文本 `UserMessage` 调用模型。

要支持“上传图片后对话”，不能只在前端增加上传按钮。图片信息需要从前端、服务端责任链、通用分发 DTO 一路传到 agent client，并在 client 侧构造成 Spring AI 多模态消息。

## 目标

- 用户在对话框内直接上传、拖拽或粘贴图片，不需要进入资源管理页面。
- 发送前能看到图片缩略图，发送后用户消息中保留图片展示。
- 服务端复用现有资源存储、鉴权和预览能力，只在聊天请求中传图片资源 ID。
- 仅视觉模型支持图片对话，非视觉模型给出明确提示或禁用上传。
- 第一版控制复杂度，不做跨轮图片自动重放和复杂图片编辑能力。

## 推荐方案

采用“对话内联上传图片，底层复用资源服务，发送时传资源 ID”的方案。

前端交互上，用户仍然停留在对话页面。选择图片后，前端自动调用资源上传接口，拿到 `resourceId`，在输入区显示缩略图。点击发送时，聊天请求携带文字和附件 ID。

请求示例：

```json
{
  "conversationId": "conv_xxx",
  "content": "帮我看看这张图有什么问题",
  "attachments": [
    {
      "resourceId": 123,
      "type": "IMAGE"
    }
  ]
}
```

## 方案对比

### 方案 A：聊天接口直接接收 multipart 文件

优点是接口看起来更直接。缺点是会让 SSE 对话接口同时承担文件上传、鉴权、消息流输出、失败重试和历史回显，复杂度高，也绕开了现有资源模块。

不推荐作为第一版。

### 方案 B：对话内联上传，聊天请求传资源 ID

优点是用户体验简单，技术上复用现有资源上传、预览、鉴权和清理能力。聊天接口仍然保持 JSON + SSE，历史消息也能通过资源 ID 回显图片。

推荐采用。

### 方案 C：图片先 OCR，再把文字发给普通模型

优点是可以兼容非视觉模型，也能降低成本。缺点是只能处理图片里的文字，无法理解物体、布局、截图上下文等视觉信息。

可作为后续降级能力，不作为主方案。

## 后端改造

### 请求对象

扩展 `AgentChatRequestVO`，增加附件列表字段：

```java
private List<AgentChatAttachmentVO> attachments;
```

新增 `AgentChatAttachmentVO`，字段建议包括：

- `resourceId`：资源 ID。
- `type`：附件类型，第一版只支持图片。

附件类型使用枚举或命名常量，避免裸写业务字符串。

### 责任链上下文

扩展 `AgentChatCommand` 和 `AgentChatContext`，把附件从 admin 接口带入智能体责任链。建议在服务端责任链前段完成附件校验和资源解析，后续 handler 只消费已验证的附件信息。

### 附件校验

新增独立的附件解析/校验服务，例如 `AgentChatAttachmentService`，负责：

- 校验资源存在。
- 校验资源属于当前用户或当前租户可访问范围。
- 校验资源类型是图片。
- 校验 MIME 类型。
- 校验单次图片数量和单张大小。
- 读取资源内容，为远程分发准备 base64。

建议第一版限制：

- 每条消息最多 3 张图片。
- 单张图片最大 5MB 或 10MB，具体值放配置。
- 支持 `image/jpeg`、`image/png`、`image/webp`。

### 模型能力校验

发送图片前需要确认当前对话模型支持视觉能力。项目已有模型扩展能力字段和默认视觉模型逻辑，建议复用现有 `capabilities` 里的 `vision` 标记。

如果模型不支持图片：

- 前端可提前禁用图片按钮。
- 后端仍需兜底校验，返回明确错误：“当前模型不支持图片对话，请切换视觉模型”。

### 分发 DTO

扩展 `ChatDispatchRequest`：

```java
private List<MediaAttachment> mediaAttachments;
```

`MediaAttachment` 建议字段：

- `resourceId`
- `fileName`
- `mimeType`
- `size`
- `base64`

服务端分发给 agent client 时携带 base64，数据库只保存资源 ID 和元数据。这样 agent client 不依赖服务端鉴权 URL，也避免远程 client 无法访问内网资源的问题。

### Agent Client Prompt 构造

修改 `DefaultPromptFactory`。当 `ChatDispatchRequest.mediaAttachments` 不为空时，构造带 Spring AI `Media` 的 `UserMessage`，而不是纯文本 `UserMessage`。

历史消息第一版仍按文本上下文发送，不自动把历史图片重新带给模型。历史图片只用于界面展示和审计。这样可以控制 token、请求体大小和模型调用成本。

## 数据存储

当前会话记录只有 `content` 等文本字段，无法表达图片附件。建议第一版给会话记录增加 JSON 扩展字段，例如 `metadata` 或 `ext_json`。

用户消息保存示例：

```json
{
  "attachments": [
    {
      "resourceId": 123,
      "type": "IMAGE",
      "name": "screenshot.png",
      "mimeType": "image/png",
      "size": 345678
    }
  ]
}
```

第一版不建议新增附件关系表。只有在后续需要附件审计、统计、批量清理或按附件检索时，再引入 `sai_agent_conversation_record_attachment`。

## 前端改造

前端仓库路径：`D:\项目案列\ai\SpringAI\snail-ai-admin`。

主要改造位置：

- `src/views/agent/detail.vue`：对话输入区增加图片入口、缩略图队列、删除按钮、拖拽上传、粘贴截图。
- `src/service/api/agent.ts`：扩展 `fetchAgentChat` 请求类型，支持 `attachments`。
- `src/typings/api/extra.d.ts`：增加聊天附件请求和会话消息附件类型。
- 会话消息渲染：用户消息中展示图片缩略图和文本。

交互要求：

- 图片按钮放在输入框工具区，使用图标按钮。
- 上传中显示加载状态，上传失败给出错误提示。
- 发送前图片以缩略图形式显示，可删除。
- 发送后图片展示在用户消息气泡中，让用户能判断“这次问的是哪张图”。
- 支持拖拽图片到对话区域。
- 支持粘贴截图。
- 非视觉模型下禁用图片入口，或发送时提示切换视觉模型。

## 数据流

1. 用户在对话框选择、拖拽或粘贴图片。
2. 前端调用资源上传接口，业务类型建议使用 `CHAT_IMAGE`。如果暂不扩展枚举，也可以先复用 `ATTACHMENT`。
3. 上传成功后，前端拿到 `resourceId` 并显示缩略图。
4. 用户输入问题并发送。
5. `/agent/{id}/chat` 接收 `content` 和 `attachments`。
6. 服务端校验模型能力和资源合法性。
7. 服务端读取图片内容，组装 `ChatDispatchRequest.mediaAttachments`。
8. agent client 构造多模态 `UserMessage` 调用模型。
9. 服务端保存用户消息、附件元数据和模型响应。
10. 前端刷新或回放历史时，通过附件元数据展示缩略图。

## 错误处理

- 图片上传失败：前端停留在当前输入状态，不发送对话。
- 图片格式不支持：提示支持的格式。
- 图片过大：提示最大大小。
- 图片数量超限：提示最多上传数量。
- 模型不支持视觉：提示切换视觉模型。
- 资源不存在或无权限：后端拒绝请求，前端提示图片已失效或无权访问。
- agent client 调用模型失败：沿用现有 SSE 错误事件。

## 安全与成本控制

- 后端必须重新校验资源归属，不能只相信前端传入的 resourceId。
- 不把图片 base64 持久化到会话记录，只在本次远程分发中使用。
- 限制图片大小和数量，避免 gRPC/JSON 请求体过大。
- 资源预览仍走现有鉴权预览接口，不直接暴露存储路径。
- 可后续增加未发送附件的定时清理逻辑，清理对话中上传但未关联消息的临时图片。

## 测试计划

### 后端

- `AgentChatRequestVO` 附件参数反序列化测试。
- 图片数量、大小、MIME 类型和权限校验测试。
- 非视觉模型发送图片的失败测试。
- `ChatDispatchRequest` 图片附件组装测试。
- 会话记录附件元数据保存和查询测试。

### Agent Client

- 无图片时保持原文本 prompt 行为。
- 有图片时构造多模态 `UserMessage`。
- 多图片顺序和 MIME 类型正确。

### 前端

- 选择图片、拖拽图片、粘贴截图。
- 上传成功后展示缩略图。
- 删除待发送图片。
- 发送时携带附件 ID。
- 历史消息回显图片。
- 非视觉模型下的禁用或错误提示。

## 分阶段实施

第一阶段：

- 对话内联上传图片。
- 发送图片 + 文本给视觉模型。
- 消息历史展示用户上传的图片。
- 服务端完成附件校验和模型能力校验。

第二阶段：

- OCR 降级能力。
- 未发送临时附件清理。
- 历史图片重新发送给模型的可选策略。
- 更多文件类型，例如 PDF、Word、Excel 附件问答。

## 待确认事项

- 第一版每条消息图片上限采用 3 张还是 5 张。
- 单张图片大小限制采用 5MB 还是 10MB。
- 资源业务类型是否新增 `CHAT_IMAGE`，还是先复用 `ATTACHMENT`。
