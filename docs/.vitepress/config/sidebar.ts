export const sidebar = {
  '/guide/': [
    {
      text: '入门',
      items: [
        { text: '项目介绍', link: '/guide/introduction' },
        { text: '快速开始', link: '/guide/quick-start' },
        { text: '核心概念', link: '/guide/concepts' },
      ],
    },
    {
      text: '智能体',
      items: [
        { text: '智能体管理', link: '/guide/agent/' },
        { text: '创建智能体', link: '/guide/agent/create' },
        { text: '配置智能体', link: '/guide/agent/configure' },
        { text: '对话交互', link: '/guide/agent/chat' },
        { text: '智能体市场', link: '/guide/agent/market' },
      ],
    },
    {
      text: '客户端模式',
      items: [
        { text: '客户端概述', link: '/guide/client/' },
        { text: '拦截器机制', link: '/guide/client/interceptor' },
        { text: 'Advisor 流水线', link: '/guide/client/advisor-pipeline' },
        { text: '客户端日志', link: '/guide/client/logging' },
        { text: '本地工具执行', link: '/guide/client/tools' },
        { text: '客户端 Chat 模式', link: '/guide/client/chat' },
        { text: '配置参考', link: '/guide/client/config' },
        { text: 'OpenAPI SDK', link: '/guide/client/openapi-sdk' },
      ],
    },
    {
      text: 'RAG 知识库',
      items: [
        { text: 'RAG 概述', link: '/guide/rag/' },
        { text: '知识库管理', link: '/guide/rag/knowledge-base' },
        { text: '文档管理', link: '/guide/rag/document' },
        { text: '分片管理', link: '/guide/rag/chunk' },
        { text: '检索配置', link: '/guide/rag/search' },
        { text: '问答对话', link: '/guide/rag/qa' },
        { text: '存储实例', link: '/guide/rag/store-instance' },
      ],
    },
    {
      text: '模型管理',
      items: [
        { text: '模型概述', link: '/guide/model/' },
        { text: '模型提供商', link: '/guide/model/provider' },
        { text: '模型配置', link: '/guide/model/config' },
        { text: '使用统计', link: '/guide/model/usage-stats' },
      ],
    },
    {
      text: 'MCP 集成',
      items: [
        { text: 'MCP 概述', link: '/guide/mcp/' },
        { text: '创建 MCP 服务', link: '/guide/mcp/create' },
        { text: '传输协议', link: '/guide/mcp/transport' },
        { text: '认证配置', link: '/guide/mcp/auth' },
      ],
    },
    {
      text: '技能系统',
      items: [
        { text: '技能概述', link: '/guide/skill/' },
        { text: '创建技能', link: '/guide/skill/create' },
        { text: '在线编辑器', link: '/guide/skill/file-editor' },
      ],
    },
    {
      text: '记忆系统',
      items: [
        { text: '记忆概述', link: '/guide/memory/' },
        { text: '记忆库配置', link: '/guide/memory/config' },
        { text: '记忆检索', link: '/guide/memory/retrieval' },
      ],
    },
    {
      text: '应用与分布式',
      items: [
        { text: '应用管理', link: '/guide/app/' },
        { text: '客户端节点', link: '/guide/app/client-node' },
        { text: '路由策略', link: '/guide/app/route-strategy' },
      ],
    },
    {
      text: '统计分析',
      items: [
        { text: '统计分析', link: '/guide/observability/analytics' },
      ],
    },
    {
      text: '其他功能',
      items: [
        { text: '资源管理', link: '/guide/resource/' },
        { text: '用户管理', link: '/guide/user/' },
        { text: '联网搜索', link: '/guide/web-search/' },
      ],
    },
  ],
  '/architecture/': [
    {
      text: '架构设计',
      items: [
        { text: '系统架构总览', link: '/architecture/overview' },
        { text: '分布式架构', link: '/architecture/distributed' },
        { text: 'Agent 责任链', link: '/architecture/agent-chain' },
        { text: 'RAG 流水线', link: '/architecture/rag-pipeline' },
        { text: '记忆架构', link: '/architecture/memory-architecture' },
        { text: '数据模型', link: '/architecture/data-model' },
      ],
    },
  ],
  '/api/': [
    {
      text: 'Admin API',
      items: [
        { text: 'API 概述', link: '/api/admin/' },
        { text: '智能体 API', link: '/api/admin/agent' },
        { text: 'RAG API', link: '/api/admin/rag' },
        { text: '模型 API', link: '/api/admin/model' },
        { text: 'MCP API', link: '/api/admin/mcp' },
        { text: '技能 API', link: '/api/admin/skill' },
        { text: '应用 API', link: '/api/admin/app' },
        { text: '资源 API', link: '/api/admin/resource' },
        { text: '用户 API', link: '/api/admin/user' },
      ],
    },
    {
      text: 'OpenAPI（外部集成）',
      items: [
        { text: 'OpenAPI 概述', link: '/api/openapi/' },
        { text: '认证方式', link: '/api/openapi/auth' },
        { text: '对话接口', link: '/api/openapi/chat' },
      ],
    },
  ],
  '/deploy/': [
    {
      text: '部署指南',
      items: [
        { text: 'Docker 部署', link: '/deploy/docker' },
        { text: '生产环境部署', link: '/deploy/production' },
        { text: '配置参考', link: '/deploy/configuration' },
        { text: '升级指南', link: '/deploy/upgrade' },
        { text: '故障排除', link: '/deploy/troubleshooting' },
      ],
    },
  ],
}
