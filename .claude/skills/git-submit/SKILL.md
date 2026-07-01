---
name: git-submit
description: 自动分析变更、更新 changelog、生成规范 commit 并推送，支持创建 Gitee PR。用户说"提交"、"git 提交"、"帮我提交代码"、"提 PR"、"分批提 PR"时使用。
license: Apache-2.0
homepage: https://gitee.com/opensnail/snail-ai
metadata:
  version: 1.0.0
  categories:
    - git
    - release
  tags:
    - commit
    - changelog
    - pr
    - gitee
---

# Git Submit Skill

Snail AI 项目的 Git 提交自动化入口。分析变更内容、生成符合规范的 commit message、更新 changelog，并支持创建 Gitee PR。

## 触发条件

当用户消息中出现以下模式时，调用本 skill：

- "提交"、"commit"、"git"、"推送"、"push"
- "提 PR"、"创建 PR"、"pull request"
- "分批提 PR"、"拆成多个 PR"
- "提交代码"、"帮我提交"、"生成 commit message"

## 工作流

1. **读取智能体文件**：`Read agents/git-submit.md`
2. **按智能体规范执行**：严格遵循 `agents/git-submit.md` 中的步骤执行
3. **报告结果**：每步完成后报告执行结果

## 模式说明

| 模式 | 触发词 | 流程 |
|------|--------|------|
| **简单提交**（默认） | "提交"、"commit" | 分析 → changelog → commit → push 到版本分支 |
| **PR 模式** | "提 PR"、"创建 PR" | 分析 → changelog → 创建特性分支 → commit → push → 创建 Gitee PR |
| **分批 PR** | "分批"、"拆成多个 PR" | 按模块拆分变更 → 每组独立执行 PR 模式 |

## 与其他 skill 的协作

- `env-config`：当 `.env` 中 `GITEE_TOKEN` 未配置时，引导用户配置
- `frontend-dev`：如果变更涉及前端仓库，changelog 中前端变更统一记入 `### snail-ai-admin` 分类
- `docs-maintenance`：大量文档变更时，可配合使用
