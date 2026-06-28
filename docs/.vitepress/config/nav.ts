export const nav = [
  { text: '首页', link: '/' },
  { text: '快速开始', link: '/guide/quick-start' },
  {
    text: '使用指南',
    items: [
      { text: '智能体管理', link: '/guide/agent/' },
      { text: '客户端模式', link: '/guide/client/' },
      { text: 'RAG 知识库', link: '/guide/rag/' },
      { text: '模型管理', link: '/guide/model/' },
      { text: 'MCP 集成', link: '/guide/mcp/' },
      { text: '技能系统', link: '/guide/skill/' },
      { text: '记忆系统', link: '/guide/memory/' },
      { text: '应用与分布式', link: '/guide/app/' },
      { text: '统计分析', link: '/guide/observability/analytics' },
    ],
  },
  {
    text: 'API 参考',
    items: [
      { text: 'Admin API', link: '/api/admin/' },
      { text: 'OpenAPI（外部集成）', link: '/api/openapi/' },
    ],
  },
  {
    text: '部署运维',
    items: [
      { text: 'Docker 部署', link: '/deploy/docker' },
      { text: '生产环境部署', link: '/deploy/production' },
      { text: '配置参考', link: '/deploy/configuration' },
      { text: '升级指南', link: '/deploy/upgrade' },
      { text: '故障排除', link: '/deploy/troubleshooting' },
    ],
  },
  { text: '常见问题', link: '/faq/' },
  { text: '加入群聊', link: '/group_chat' },
  { text: '赞助', link: '/support' },
  {
    text: '更多',
    items: [
      { text: '更新日志', link: '/changelog' },
      { text: '待实现功能', link: '/upcoming' },
      { text: '参与贡献', link: '/contributing' },
      { text: '路线图', link: '/roadmap' },
    ],
  },
]
