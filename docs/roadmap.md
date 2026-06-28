# 路线图

本页展示 Snail AI 的功能规划和开发进展。路线图按里程碑组织，具体功能以当前源码、发布说明和初始化脚本为准。

**状态说明：**

- 已完成：当前源码已落地，可在文档中作为可用能力描述。
- 开发中：已有方向或部分实现，使用前需要按源码验证。
- 已规划：规划方向，不应写入部署主流程。

## v0.0.6 -- 当前版本能力

| 能力 | 状态 | 说明 |
|------|------|------|
| 智能体管理 | 已完成 | 支持智能体创建、配置、对话和市场相关能力 |
| Server-Agent 架构 | 已完成 | Server 通过 gRPC 与 Agent Client 通信 |
| OpenAPI 外部集成 | 已完成 | 使用 `Snail-Ai-App-Id` + `Snail-Ai-Token` 认证 |
| Chat 模型 | 已完成 | OpenAI-compatible Chat |
| Embedding 模型 | 已完成 | OpenAI-compatible Embedding |
| Rerank 模型 | 已完成 | Qwen/HTTP Rerank |
| RAG 知识库 | 已完成 | 文档解析、分片、向量化、检索和重排链路 |
| MCP 工具集成 | 已完成 | 支持 SSE、Streamable HTTP、Stdio 等接入方式 |
| Skill 技能系统 | 已完成 | 支持 Skill 定义和资源加载 |
| 短期记忆 | 已完成 | 支持 `memory` / `db` 存储模式 |
| MySQL / PostgreSQL | 已完成 | 初始化脚本位于 `docs/sql/` |
| PgVector / Milvus / Elasticsearch | 已完成 | Compose 依赖位于 `docs/docker/docker-compose.yaml` |
| 文件存储 | 已完成 | 支持本地资源目录和 MinIO |
| 统计分析与客户端日志 | 已完成 | 支持智能体统计分析、客户端日志、Token 用量和思维内容采集 |

## v0.1.x -- 能力增强（规划中）

### 工作流 / DAG 编排引擎

- 可视化拖拽式流程编排。
- 支持条件分支、循环、并行等流程控制。
- 工作流与智能体组合调用。

### 更多模型接入方式

- 在 OpenAI-compatible 之外补充更多原生 Provider 适配。
- 对兼容接口做更完整的自动化测试。
- 完善不同模型类型的参数校验和错误提示。

### Webhook 通知机制

- 对话完成事件通知。
- 智能体状态变更通知。
- 自定义 Webhook 端点配置。
- 重试机制和签名验证。

### Agent 间协作

- 多 Agent 对话编排。
- Agent 之间的消息传递和任务委托。
- 主从 Agent 模式。

### 增强 RAG

- OCR 支持。
- 表格智能提取。
- 知识图谱集成。
- 多模态文档理解。

## v0.2.x -- 企业增强（规划中）

### 多租户隔离

- 租户级数据隔离。
- 租户配额管理。
- 租户级模型配置和权限控制。

### SSO 单点登录集成

- LDAP / Active Directory 集成。
- OIDC / OAuth 2.0 标准协议。
- 企业微信 / 钉钉 / 飞书第三方登录。

### 审计日志

- 用户操作审计记录。
- 敏感操作告警。
- 审计日志导出和归档。

### 自动化评估

- 基于数据集的自动化测试。
- 准确率、相关性、忠实度等评估指标。
- 回归测试和持续监控。

## v0.3.x -- 生态扩展（规划中）

### 插件市场

- 社区插件发布和安装。
- 插件沙箱安全机制。
- 插件版本管理和依赖解析。

### 移动端支持

- 移动端响应式适配。
- 微信小程序 / H5 对话入口。
- 移动端推送通知。

### 多语言 SDK

- Python Agent Client SDK。
- Go Agent Client SDK。
- Node.js Agent Client SDK。

### 国产化与多数据库适配

- SQL Server、达梦、MariaDB 等数据库适配。
- 国产操作系统和 CPU 架构验证。
- 信创环境部署文档和测试矩阵。

## 长期愿景

- 成为 Java 生态中成熟的开源 AI Agent 平台。
- 打通从开发、部署到运维的完整 AI Agent 生命周期。
- 建立活跃的社区生态，提供丰富的插件、技能和集成模板。

## 参与规划

路线图中的功能优先级会根据社区反馈动态调整。你可以通过 Issue 提出建议、反馈问题或认领感兴趣的功能模块。

## 相关源码

- `pom.xml`
- `snail-ai-starter/src/main/resources/application.yml`
- `snail-ai-server/`
- `snail-ai-agent/`
- `snail-ai-models/`
- `docs/sql/`
- `docs/docker/docker-compose.yaml`
