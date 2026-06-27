---
name: docs-maintenance
description: Snail AI 文档维护智能体入口。用户要求执行文档智能体、维护/校验/更新 docs 文档站点、同步接口/配置/脚本变更到文档、构建或打包文档时必须使用；尤其是提到“文档智能体”“docs”“VitePress”“文档站点”“打包文档”“校验文档”时，先调用本 skill。
license: Apache-2.0
homepage: https://gitee.com/opensnail/snail-ai
metadata:
  version: 0.0.6
  categories:
    - documentation
    - vitepress
    - release
  tags:
    - docs
    - source-validation
    - packaging
---

# Snail AI Docs Maintenance Skill

Use this skill as the Claude Code entry point for the repository document-maintenance agent.

## Workflow

1. Read `agents/docs-maintenance.md` from the repository root.
2. Follow that agent file as the authoritative workflow for documentation maintenance.
3. Before changing docs, verify implementation facts from source code, scripts, configuration, and VitePress config as directed by the agent.
4. Keep changes focused on documentation, VitePress configuration, and documentation packaging scripts unless the user explicitly asks for source-code changes.
5. Do not commit generated `docs/dist/` or `docs/node_modules/` outputs.

## When asked to run the document agent

Treat requests such as “执行文档智能体”, “跑一下文档智能体”, “维护 docs”, or “检查文档站点” as instructions to execute the workflow in `agents/docs-maintenance.md`.

## Validation

Run the checks listed in `agents/docs-maintenance.md` when practical. If a command cannot be run because dependencies are missing, permissions are denied, or the user only asked for a plan, report that explicitly instead of claiming success.

## Reporting

Summarize:

- Documentation areas changed or checked.
- Validation commands run and their results.
- Any skipped checks or intentionally retained references.
