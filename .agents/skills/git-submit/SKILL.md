---
name: git-submit
description: 快速本地提交。用户说"提 PR"时切换 PR 模式。
---

# Git Submit

## 本地提交（默认）

用户说"提交"、"commit"时执行，一步到位，不墨迹。

### 流程

1. **`git add -A`** 暂存所有变更
2. **读版本号**：`grep '<revision>' pom.xml | head -1 | sed 's/.*<revision>//;s/<\/revision>.*//'`
3. **生成 commit message**：`<type>(<版本号>): <中文描述>`
   - type 从 diff 自动判断：`feat`/`fix`/`docs`/`refactor`/`chore`
   - 描述要简洁，一行概括
4. **直接 `git commit`**，不废话确认（除非用户指定了范围）
5. **报告 commit SHA**

### 规则

- 版本号从 pom.xml 读，不硬编码
- 变更范围为空时终止，提示"没有需要提交的变更"
- 用户指定了 type/scope/message 时，以用户为准
- 用户说"只提交 X"时，只 add 指定文件

## PR 模式

用户说"提 PR"、"创建 PR"、"pull request"时启用。

1. 从当前版本分支创建特性分支：`<type>/<scope>-<简述>`
2. 执行本地提交流程
3. `git push -u origin <分支名>`
4. 通过 Gitee API 创建 PR：
   - Token 从 `.env` 的 `GITEE_TOKEN` 读取
   - 仓库 owner/repo 从 `git remote get-url origin` 解析
   - 用 Write 工具写 JSON 到临时文件，`curl --data-binary @file` 发送
   - API: `POST https://gitee.com/api/v5/repos/<owner>/<repo>/pulls?access_token=$TOKEN`
5. 输出 PR 链接

## 更新 Changelog

仅当用户明确说"更新 changelog"时执行。feat/fix 类变更追加到 `docs/changelog.md` 当前版本区块下，用用户能听懂的语言描述。