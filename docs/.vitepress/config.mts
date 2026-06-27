import { defineConfig } from 'vitepress'
import { withMermaid } from 'vitepress-plugin-mermaid'
import { nav } from './config/nav'
import { sidebar } from './config/sidebar'

export default withMermaid(
  defineConfig({
    title: 'Snail AI',
    description: '企业级 AI Agent 平台 —— 让 AI 更智能，让开发更高效',
    outDir: './dist',
    assetsDir: 'static',
    markdown: {
      lineNumbers: true,
    },
    ignoreDeadLinks: true,
    head: [
      ['link', { rel: 'icon', href: '/favicon.ico' }],
      ['meta', { name: 'keywords', content: 'Snail AI, AI Agent, RAG, MCP, Spring AI, 智能体, 开源AI平台' }],
      ['meta', { name: 'author', content: 'OpenSnail' }],
    ],
    vite: {
      resolve: {
        alias: {
          'dayjs/plugin/advancedFormat.js': 'dayjs/esm/plugin/advancedFormat',
          'dayjs/plugin/customParseFormat.js': 'dayjs/esm/plugin/customParseFormat',
          'dayjs/plugin/isoWeek.js': 'dayjs/esm/plugin/isoWeek',
        },
      },
      optimizeDeps: {
        include: ['mermaid', 'dayjs'],
      },
    },
    themeConfig: {
      siteTitle: 'Snail AI',
      logo: '/images/logo.svg',
      nav,
      sidebar,
      search: {
        provider: 'local',
      },
      outline: {
        level: 'deep',
        label: '目录',
      },
      socialLinks: [
        {
          icon: {
            svg: '<svg x="5" y="5" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="24" height="24"><path d="M512 1024C229.222 1024 0 794.778 0 512S229.222 0 512 0s512 229.222 512 512-229.222 512-512 512z m259.149-568.883h-290.74a25.293 25.293 0 0 0-25.292 25.293l-0.026 63.206c0 13.952 11.315 25.293 25.267 25.293h177.024c13.978 0 25.293 11.315 25.293 25.267v12.646a75.853 75.853 0 0 1-75.853 75.853h-240.23a25.293 25.293 0 0 1-25.267-25.293V417.203a75.853 75.853 0 0 1 75.827-75.853h353.946a25.293 25.293 0 0 0 25.267-25.292l0.077-63.207a25.293 25.293 0 0 0-25.268-25.293H417.152a189.62 189.62 0 0 0-189.62 189.645V771.15c0 13.977 11.316 25.293 25.294 25.293h372.94a170.65 170.65 0 0 0 170.65-170.65V480.384a25.293 25.293 0 0 0-25.293-25.267z" fill="#C71D23"></path></svg>',
          },
          link: 'https://gitee.com/aizuda/snail-ai',
        },
      ],
      editLink: {
        pattern: 'https://gitee.com/opensnail/snail-ai-docs/edit/master/docs/:path',
        text: '在 Gitee 上编辑此页',
      },
      lastUpdated: {
        text: '上次更新',
      },
      docFooter: {
        prev: '上一篇',
        next: '下一篇',
      },
      footer: {
        message: 'Apache 2.0 Licensed',
        copyright: 'Copyright © 2024-present <a href="https://gitee.com/opensnail">OpenSnail</a>',
      },
    },
  })
)
