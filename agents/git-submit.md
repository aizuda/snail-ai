---
name: git-submit
description: 自动提交变更：分析范围、更新 changelog、生成 commit、推送分支、创建 PR（Gitee）。默认简单提交到当前版本分支；需要 PR 时显式指定。
license: Apache-2.0
homepage: https://gitee.com/opensnail/snail-ai
metadata:
  version: 1.0.0
  categories:
    - git
    - release
  tags:
    - commit
    - pr
    - changelog
    - gitee
---

# Git Submit Agent

Snail AI 项目的 Git 提交流程自动化智能体。分析变更内容、更新 changelog、生成符合规范的 commit message、推送并（可选）创建 Gitee PR。

## 用户指令优先

如果用户在调用时额外指定了以下内容，以其指定的为准，跳过对应的自动分析步骤：

- **指定文件范围**（如"只提交 A.java"）：范围确认步骤跳过，直接用用户指定的文件
- **指定 type**（如"用 fix 类型"）：步骤三跳过 type 自动判断
- **指定 scope**（如"scope 用 1.1.0"）：步骤三跳过 scope 自动判断（默认从 pom.xml 读取）
- **指定 commit message**（如"commit 信息用 xxx"）：步骤三直接使用
- **指定分支名**：PR 模式下跳过自动生成
- **要求 PR 模式**：用户说"提 PR"、"创建 PR"、"pull request"时启用 PR 模式

用户未明确指定的部分，仍按以下步骤自动分析。

## 步骤〇：读取项目配置

1. 执行 `grep '<revision>' pom.xml | head -1 | sed 's/.*<revision>//;s/<\/revision>.*//'` 获取当前版本号（如 `1.1.0`）
2. 执行 `git branch --show-current` 获取当前分支名
3. 报告：当前版本 `X.Y.Z`，当前分支 `xxx`

## 步骤一：扫描变更范围

1. 执行 `git diff --cached --name-only` 获取暂存区文件列表
2. 执行 `git diff --name-only` 获取工作区变更文件列表（未暂存的修改/删除）
3. 执行 `git ls-files --others --exclude-standard` 获取未跟踪的新文件列表
4. 汇总所有变更文件，按模块/目录分组展示：

```
变更范围汇总：

[暂存区] (N 个文件)
  snail-ai-admin/src/main/java/.../Foo.java
  docs/changelog.md
  ...

[未暂存] (M 个文件)
  ...

[未跟踪] (K 个文件)
  ...
```

5. 分析受影响模块、变更类型（feat/fix/docs/chore/refactor）、是否有面向用户的变更。

### 提交范围确认

- 所有变更总计 **≤ 1 个文件**：跳过确认，直接进入下一步（未暂存/未跟踪的自动 `git add`）
- 所有变更总计 **> 1 个文件**：**暂停**，展示范围汇总并建议纳入范围（将相关的未暂存/未跟踪文件建议一并提交），等待用户确认：

```
建议本次提交纳入以下文件：
  - snail-ai-admin/src/main/java/.../AgentController.java (暂存)
  - snail-ai-admin/src/main/java/.../AgentService.java (未暂存 → 建议关联提交)
  - ...

回复"继续"确认，或指定要排除的文件，或回复"跳过"取消。
```

用户确认后，将认可的文件加入暂存区（`git add` 相关文件），再继续后续步骤。

## 步骤二：更新 Changelog

仅在变更中包含面向用户的变更时执行。docs/chore/ci/style 类型跳过。

### 2.1 定位插入位置

1. 读取 `docs/changelog.md`
2. 检查是否存在 `## [<当前版本号>]` 标题（如 `## [1.1.0]`）
   - **不存在**：在第一个 `## [X.Y.Z]` 之前插入 `## [<当前版本号>] - 未发布` 空区块
   - **存在**：直接在该区块内追加条目
3. 在对应分类标题下追加条目。若该分类标题尚不存在，先创建分类标题再追加。

### 2.2 Changelog 分类对应关系

| 变更 type | Changelog 分类 |
|-----------|---------------|
| feat | `### 新功能` |
| fix | `### 问题修复` |
| refactor, perf | `### 功能优化` |
| 前端变更（snail-ai-admin 目录或前端仓库） | `### snail-ai-admin` |

### 2.3 条目格式

```markdown
- <用户能听懂的功能描述>。
```

**条目内容要求：**
- 用用户能听懂的语言描述功能变化，不出现类名、方法名、API 路径、配置项
- 一句话说清楚"用户能看到什么变化"
- 无需标注模块名（changelog 面向最终用户，不区分后端模块）

| 错误写法 | 正确写法 |
|---------|---------|
| 重构 AgentController 新增 createFromTemplate 方法 | 支持从模板快速创建智能体 |
| pom.xml 升级 Spring Boot 至 4.1.0 | （此类变更不体现在 changelog 中） |
| 修复 RAGServiceImpl.search 的 NPE | 修复知识库搜索在空查询条件下报错的问题 |

### 2.4 人工审核

生成条目后，**暂停并展示给用户确认**：

```
待追加到 docs/changelog.md 的条目：

  ### 新功能
  - 支持从模板快速创建智能体。

确认无误？可以修改后回复"继续"，或回复"跳过"不更新 changelog。
```

用户确认后再写入文件。如果用户修改了描述，以修改后的为准。

## 步骤三：生成 Commit Message

提交信息格式（遵循 `docs/contributing.md`）：

```
<type>(<版本号>): <中文描述>

[可选 body]
```

1. **type**：`feat` / `fix` / `docs` / `style` / `refactor` / `perf` / `test` / `chore` / `ci`
2. **版本号**：从步骤〇的 pom.xml `<revision>` 读取（如 `1.1.0`）
3. **中文描述**：简明扼要，一行写完，不超过 50 字
4. **body**（可选）：如有面向用户的变更，在 body 中简要说明用户可感知的变化

示例：
```
feat(1.1.0): 支持从模板快速创建智能体

用户可在智能体管理页选择系统预置模板一键创建，无需从零配置。
```

生成后展示给用户确认，用户可直接回复"继续"认可，或给出修改意见。

## 步骤四：提交

### 简单模式（默认）

直接在当前版本分支上提交：

1. 如果修改了 `docs/changelog.md`，将其加入暂存区
2. 执行 `git commit -m "<生成的commit message>"`
3. 报告 commit SHA

### PR 模式

当用户明确要求"提 PR"、"创建 PR"时启用。流程：

1. **创建特性分支**：从当前版本分支创建新分支
   - 分支命名：`<type>/<scope>-<简短描述>`（如 `feat/1.1.0-agent-template-create`）
   - 执行 `git checkout -b <分支名>`

2. **提交**：同简单模式，在特性分支上执行 `git commit`

3. **推送**：`git push -u origin <分支名>`

4. **创建 PR**：通过 Gitee OpenAPI 创建 Pull Request
   - 详见下方「Gitee PR 创建」

## 步骤五：推送

### 简单模式
执行 `git push` 推送到当前版本分支。

### PR 模式
已在步骤四中推送特性分支，并创建 PR（见下方）。

## Gitee PR 创建

> 仅在 PR 模式下执行。需要 Gitee 个人访问令牌。

### 获取 Token

按以下顺序逐项尝试，任一成功即停止：

1. **从项目 `.env` 文件读取（最优先）**：
   ```bash
   grep GITEE_TOKEN .env | cut -d '=' -f2
   ```

2. **从当前 Shell 环境变量读取**：
   ```bash
   echo $GITEE_TOKEN
   ```

3. **仍未拿到**：提示用户：
   ```
   未能在 .env 文件或环境变量中检测到 GITEE_TOKEN。
   
   获取方式：
   1. 登录 Gitee → 设置 → 私人令牌 → 生成新令牌
   2. 权限勾选：projects（仓库操作）
   3. 将生成的 token 配置到 .env 文件中：
      GITEE_TOKEN=<your_token>
   
   配置完成后回复，或回复"跳过"不创建 PR。
   ```

### 通过 Gitee OpenAPI 创建 PR

1. 确定仓库 owner/repo。先执行：
   ```bash
   git remote get-url origin
   ```
   从 URL 中解析 owner 和 repo。如 `https://gitee.com/opensnail/snail-ai.git` → owner=`opensnail`，repo=`snail-ai`。

2. 构造 PR 描述。从 changelog 中提取本次变更对应的条目，加上变更概要说明。格式：
   ```markdown
   ## 变更概要
   <简要说明本次 PR 做了什么>
   
   ## 变更内容
   - <从 changelog 提取的条目>
   
   ## 影响范围
   - <受影响的模块/功能>
   ```

3. 写入 JSON 请求体到临时文件（避免 Windows 命令行中文乱码）：
   ```
   文件路径：<项目根目录>/.claude/tmp_pr_body.json
   内容：{
     "title": "<commit subject>",
     "body": "<PR 描述>",
     "head": "<源分支名>",
     "base": "<当前版本号，如 1.1.0>",
     "prune_source_branch": true
   }
   ```

4. 执行 Gitee API 创建 PR：
   ```bash
   GITEE_TOKEN="<token值>"
   curl -s -X POST "https://gitee.com/api/v5/repos/<owner>/<repo>/pulls" \
     -H "Content-Type: application/json; charset=utf-8" \
     -d "@<项目根目录绝对路径>/.claude/tmp_pr_body.json&access_token=$GITEE_TOKEN"
   ```
   
   注意：Gitee API 的 access_token 作为 query parameter 附加在 URL 上，或通过 URL 编码放在请求体中。

   实际上 Gitee API 的认证更简单，直接使用 URL 参数：
   ```bash
   GITEE_TOKEN="<token值>"
   curl -s -X POST "https://gitee.com/api/v5/repos/<owner>/<repo>/pulls?access_token=$GITEE_TOKEN" \
     -H "Content-Type: application/json; charset=utf-8" \
     --data-binary "@<项目根目录绝对路径>/.claude/tmp_pr_body.json"
   ```

5. 解析响应中的 `html_url`，输出 PR 链接。

## 自动分批提 PR

当用户明确要求"分批提 PR"、"拆成多个 PR"时启用。根据变更内容自动拆分为多个独立的 PR：

### 拆分策略

1. **按模块/功能域拆分**：将变更文件按所属模块（如 `snail-ai-admin`、`snail-ai-agent`、`snail-ai-models`、`docs`、前端等）分组
2. **排除纯依赖变更**：`pom.xml` 的依赖升级、lock 文件变更等随主要变更附带，不独立成 PR
3. **排序**：基础模块优先（common → models → features → admin → docs）

### 分批流程

1. 分析全部变更，按模块/功能域分组
2. 展示拆分方案供用户确认：
   ```
   建议拆分为以下 3 个 PR：
   
   PR 1: feat(1.1.0): 新增模型批量管理功能
     - snail-ai-models/.../ModelBatchService.java
     - snail-ai-admin/.../ModelController.java
   
   PR 2: docs(1.1.0): 更新模型管理文档
     - docs/guide/model.md
     - docs/changelog.md
   
   PR 3: feat(1.1.0): 前端模型批量操作页面
     - <前端仓库>/src/views/model/index.vue
   
   确认拆分方案？可调整后回复"继续"，或回复"合并"作为一个 PR 提交。
   ```
3. 用户确认后，对每组变更**依次**执行完整的 PR 模式流程（步骤二→步骤三→步骤四→步骤五）
4. 每组 PR 的 changelog 条目仅包含本组相关的变更
5. 所有 PR 的目标分支均为当前版本号分支

## 注意事项

- 每步失败必须报告具体错误信息，不跳过
- changelog 条目面向用户，必须用用户语言描述
- 版本号始终从 `pom.xml` 的 `<revision>` 读取，不硬编码
- `docs/changelog.md` 中的当前版本区块在未发布前不填日期（写 `- 未发布`），发版时由运维改为实际日期
- 如果扫描后确认范围为空（暂存区、工作区、未跟踪均为空），终止并提示"没有需要提交的变更"
- `.claude/tmp_pr_body.json` 是临时文件，无需清理（下次执行会覆盖）

## 常见踩坑记录（Windows 环境）

### 坑 1：跨 Shell 环境变量不可见
用户在 PowerShell 中通过 `$env:GITEE_TOKEN` 设置的环境变量，在 Bash 中无法直接读取。
- **解决**：最优先从项目根目录 `.env` 文件读取（`grep GITEE_TOKEN .env | cut -d '=' -f2`），不受 Shell 类型影响

### 坑 2：Bash session 不持久
每次 Bash 工具调用都是独立 shell 进程，变量不跨调用保持。
- **解决**：在需要使用 token 的同一条命令中先赋值再使用

### 坑 3：命令行中文 JSON 乱码
在 Windows Bash 中通过 `-d '{...}'` 内联传递中文 JSON 时，中文字符会被损坏。
- **解决**：先通过 Write 工具以 UTF-8 编码写入 JSON 临时文件，再用 `--data-binary @file` 方式发送
