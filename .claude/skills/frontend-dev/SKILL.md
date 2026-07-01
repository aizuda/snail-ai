---
name: frontend-dev
description: Snail AI Admin 前端开发技能入口。涉及前端页面/组件/API/类型的变更时调用，自动定位前端源码目录并应用前端编码规范。
license: Apache-2.0
homepage: https://gitee.com/opensnail/snail-ai
metadata:
  version: 1.0.0
  categories:
    - frontend
    - vue
    - typescript
  tags:
    - vue3
    - naive-ui
    - frontend-sync
---

# Snail AI Admin 前端开发 Skill

当用户的需求涉及 snail-ai-admin 前端项目时使用本 skill 作为入口。

## 工作流

1. **定位前端源码**：读取项目根目录 `.env` 中的 `SNAIL_AI_FRONTEND_PATH` 变量
   - 若 `.env` 不存在、变量未设置、或路径无效 → 使用 `env-config` skill 引导用户配置
   - 验证通过后，以该路径作为前端项目根目录（后续记为 `$FRONTEND`）

2. **读取智能体文件**：`Read agents/frontend-dev.md`，将其作为前端开发的权威规范

3. **按智能体规范执行**：
   - 遵循 `agents/frontend-dev.md` 中的编码规范、项目结构、组件模式
   - 执行后端同步工作流（API 层 → 类型定义 → 页面组件 → 国际化 → 路由）

4. **变更完成后验证**：
   ```bash
   cd $FRONTEND && pnpm typecheck
   cd $FRONTEND && pnpm lint
   ```

## 触发条件

当用户消息中出现以下模式时，调用本 skill：

- "前端"、"页面"、"界面"、"UI"、"组件"
- "同步前端"、"更新前端"、"前端也要改"
- "新增页面"、"加个页面"、"前端加个功能"
- "修复前端 Bug"、"前端报错"
- 新增/修改后端 Controller 后自动关联前端变更
- 提到 snail-ai-admin 或 Vue 相关关键词

## 其他工具集成

本 skill 默认使用 Claude Code 内置能力执行前端开发任务。如需使用其他工具：

- **Codex**：在对话中 `@agents/frontend-dev.md` 引用本智能体文件
- **其他 AI 编码工具**：将 `agents/frontend-dev.md` 作为 system prompt 或规则文件加载

## 与其他 skill 的协作

- `env-config`：前置依赖，确保 `SNAIL_AI_FRONTEND_PATH` 配置正确
- `dev-guide`：后端 Java 编码规范，前后端联动时配合使用
