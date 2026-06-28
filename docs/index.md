---
layout: home

hero:
  name: "Snail AI"
  text: "企业级 AI Agent 平台"
  tagline: 让 AI 更智能，让开发更高效
  image:
    src: /images/home.svg
    alt: Snail AI
  actions:
    - theme: brand
      text: v0.0.6 正式版本 全新发布
      link: /guide/quick-start
    - theme: alt
      text: Gitee 仓库
      link: https://gitee.com/aizuda/snail-ai

features:
  - icon: 🤖
    title: 智能体管理
    details: AI 辅助一键创建，责任链架构灵活编排，可视化配置提示词与预设问题，内置市场支持发布与订阅
  - icon: 🔌
    title: 客户端自主可控
    details: 拦截器深度介入 AI 交互全流程，自定义请求/响应处理，在线日志实时查看，本地工具执行确保数据不出域
  - icon: 📚
    title: RAG 知识库
    details: 10+ 文档格式，4 种分片策略，混合检索（向量 + BM25 + 融合 + 重排），文档智能去重
  - icon: 🧠
    title: 多模型支持
    details: 统一管理 OpenAI-compatible 对话/嵌入模型与 Qwen/HTTP 重排模型，兼容端点可按实际协议接入
  - icon: 🗄️
    title: 多数据库适配
    details: MySQL、PostgreSQL、Dameng 已支持，PgVector、Milvus、Elasticsearch 可用于向量/检索存储
  - icon: 🛠️
    title: MCP 工具集成
    details: 完整 MCP 协议支持（SSE / Streamable HTTP / Stdio），多种认证方式，一键连接测试
  - icon: 📊
    title: 统计分析
    details: 智能体维度活跃用户、对话数、消息数和用户使用明细统计
---

<style>
:root {
  --vp-home-hero-name-color: transparent;
  --vp-home-hero-name-background: -webkit-linear-gradient(120deg, #bd34fe 30%, #41d1ff);

  --vp-home-hero-image-background-image: linear-gradient(-45deg, #d7dbf6 50%, #d7dbf6 50%);
  --vp-home-hero-image-filter: blur(40px);
}

@media (min-width: 640px) {
  :root {
    --vp-home-hero-image-filter: blur(56px);
  }
}

@media (min-width: 960px) {
  :root {
    --vp-home-hero-image-filter: blur(72px);
  }
}
.m-home-layout .image-src:hover {
  transform: translate(-50%, -50%) rotate(666turn);
  transition: transform 59s 1s cubic-bezier(0.3, 0, 0.8, 1);
}

.m-home-layout .details small {
  opacity: 0.8;
}

.m-home-layout .bottom-small {
  display: block;
  margin-top: 2em;
  text-align: right;
}
</style>

## 系统架构

```mermaid
graph TB
    subgraph 前端["🖥️ 前端 (Vue 3 + Naive UI)"]
        Admin[管理后台]
        Chat[对话界面]
    end

    subgraph 服务端["⚙️ Server (Spring Boot 4)"]
        API[REST API / SSE :8900]
        Chain[Agent 责任链]
        RAG[RAG 引擎]
        OpenAPI[OpenAPI 外部接口]
    end

    subgraph 客户端["🤖 Agent Client (gRPC)"]
        SDK[Agent SDK]
        Interceptor[拦截器链]
        Tools[本地工具执行]
        LLM[LLM 调用]
    end

    subgraph 存储["💾 数据存储"]
        DB[(MySQL/PostgreSQL/Dameng)]
        Vector[(PgVector/Milvus/ES)]
        Storage[(MinIO/本地)]
    end

    Admin --> API
    Chat --> API
    API --> Chain
    Chain -->|gRPC 双向流| SDK
    SDK --> Interceptor --> LLM
    SDK --> Tools
    Chain --> RAG --> Vector
    API --> DB
    OpenAPI --> Chain
    RAG --> Storage
```
