# Snail AI - Claude Code 编码规范

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

## 其他个规范
其他的开发规范请查看【.claude/skills】下面的规范信息