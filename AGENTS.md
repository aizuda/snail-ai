# Snail AI - Codex 编码规范

## Java 代码约定

编写或修改 `**/*.java` 时遵守以下约定。

### 1. 魔法值

- 业务状态、类型、配置键等：**禁止**裸写 `"ACTIVE"`、`1` 等无注释字面量。
- 优先 **`enum`**；其次 **命名常量**（`static final` 或集中常量类）。

### 2. 方法参数个数

- 公开方法（public / 对外服务 API）参数 **超过 3 个** 时，改为 **单一请求/上下文对象**（DTO、Command、`*Request`、`*Context`）。
- 私有小工具方法可酌情保持多参。

### 3. 分支与复杂度

- 优先 **卫语句 + 提前返回**，避免深嵌套 `if`。
- 多分支按「类型/策略」区分时，考虑 **策略模式** 或 **多态**，避免超长 `if-else`。
- 复杂条件抽取为 **具名方法** 或 **小类**。

### 4. 设计模式（按需）

- **单一职责**：类过大则拆分。
- **策略 / 工厂 / 建造者 / 门面**：在需要扩展、隐藏创建细节、统一入口时使用；避免空泛套用。

### 5. 与项目一致

- 新代码与现有包结构、命名风格（如 `*PO`、`*Service`、`*Facade`）保持一致。
- 遵循现有模块分层：`controller` → `service` → `mapper`/`repository`。
- 使用项目已有的工具类和通用组件，不重复造轮子。

## 项目结构

- 后端：Spring Boot + MyBatis-Plus，模块化 Maven 项目
- 前端：Vue 3 + TypeScript + Ant Design Vue，位于 `snail-ai-web/`
- 前端页面使用 `useTable` + `useTableOperate` + TSX `customRender` 标准模式
- API 服务层按功能模块拆分（`model.ts`、`provider.ts` 等）

## 通用规则

- 提交信息格式：`feat/fix/refactor(scope): 中文描述`
- 不要修改不相关的代码，保持变更最小化
- 新增 API 端点时同步更新前端 service 层类型定义

## 开发规范 Skill

- 每次编写或修改代码前，必须先阅读 `.claude/skills/dev-guide/SKILL.md`。
- 按任务类型继续阅读对应细则：架构调整读 `architecture.md`，Java/编码风格读 `coding-standards.md`，数据库/Mapper/PO 调整读 `database-guide.md`，接口调整读 `api-design.md`，命名不确定时读 `naming-conventions.md`。
- `.claude/skills/dev-guide/` 中的规范与本文件同时生效；如与当前开源版实际模块结构冲突，以当前代码结构和本文件的开源版约束为准。

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **snail-ai** (14934 symbols, 33408 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({search_query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.
- For security review, `explain({target: "fileOrSymbol"})` lists taint findings (source→sink flows; needs `analyze --pdg`).

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/snail-ai/context` | Codebase overview, check index freshness |
| `gitnexus://repo/snail-ai/clusters` | All functional areas |
| `gitnexus://repo/snail-ai/processes` | All execution flows |
| `gitnexus://repo/snail-ai/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
