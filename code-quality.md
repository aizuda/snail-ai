# Snail AI 代码质量改进清单

> 扫描范围：`com.aizuda.**` 业务代码 + vendored 模型层。生成产物（protobuf gRPC、前端编译产物）不计入。
> 总体结论：**基础卫生良好**（全仓库 `printStackTrace` 仅 1 处、`System.out` 仅 2 处、`TODO` 仅 1 处、`else-if` 链很少），主要问题集中在**少数超大类 / 长方法、魔法值、日志规范不统一、缺少自动化质量门禁**。
> 当前项目**无任何代码格式化 / 静态检查工具**（无 Spotless / Checkstyle / PMD / SpotBugs / EditorConfig）。

---

## 一、复杂度热点（高优先级）

### 1.1 超大类（单一职责风险）
按行数排序的业务类 Top（已剔除 protobuf 生成类）：

| 文件 | 行数 | 问题 | 建议 |
|------|------|------|------|
| ~~`SkillService.java`~~ | ~~965~~ | ✅ 已拆分（2026-07-20）：门面 537 行 + `SkillPackageService`(207) + `SkillFileService`(398) + `SkillMarkdownParser`(114)，public 签名与事务边界不变 | 完成 |
| ~~`AiModelConfigService.java`~~ | ~~663~~ | ✅ 已拆分（2026-07-20）：门面 367 行 + `ModelConfigValidator`(186) + `ModelCredentialCipher`(76) + `ModelConnectivityTester`(129，由 ModelTestService 更名归并) + `ModelConfigQueryService`(310) | 完成 |
| ~~`RagDocumentService.java`~~ | ~~752~~ | ✅ 已拆分（2026-07-20）：门面 393 行 + `RagDocumentUploadService`(306) + `RagDocumentProcessOrchestrator`(195) + `RagDocumentAssembler`(125) | 完成 |
| `UserService.java` | 586 | 见 task.md 安全项；同时口令加解密 / JWT / 用户 CRUD 混杂 | 抽 `PasswordEncoder` / `TokenService` 与用户 CRUD 解耦（**与 task.md A1 安全整改一并做**，避免两次动认证逻辑） |
| `ClientInstanceManager.java` | 526 | 客户端实例状态管理 | 评估状态机抽取 |
| ~~`AgentService.java`~~ | ~~590~~ | ✅ 已拆分（2026-07-20）：门面 342 行 + `AgentValidator`(88) + `AgentAssembler`(319)，调用方零改动 | 完成 |

> 阈值建议：**单类 > 400 行**、**单方法 > 80 行**触发拆分评审。

### 1.2 长方法 + 深层嵌套
- `OpenAiChatModel.createRequest`（vendored，约 **400 行单方法**）：`if/else if + 多层 forEach + instanceof` 深度嵌套，承担「消息转换 + 媒体处理 + 选项映射 + 工具装配 + 流式选项」全部职责。
  - **注意：这是 vendored 的 Spring AI 源码**（Apache 头 + 上游作者署名，包名 `org.springframework.ai.openai`）。**不建议大改**，否则后续同步上游会持续冲突。处理策略见第五节。
  - 若确需本地增强，建议「薄封装 / 子类 / 适配器」隔离自有逻辑，而非直接改原方法。
- 业务侧建议：消息类型分发（USER/SYSTEM/ASSISTANT/TOOL）这类「按类型分支」逻辑，统一用**策略 / 多态**替代长 `if-else if`（与团队编码规范一致）。

---

## 二、魔法值与枚举（中优先级）

- vendored `OpenAiChatModel` 中大量裸字符串：`"image/"`、`"audio/"`、`"mp3"`、`"auto"`、`"none"`、`"required"`、`"function"`、`"reasoning_content"`、`"strict"` 等——vendored 不动，但**自有代码不得效仿**。
- 排查重点（按团队规范「业务状态 / 类型 / 配置键禁止裸字面量」）：
  - 状态码 / 角色 / 类型这类 `Integer` / `String` 字面量，统一收敛到 `enum` 或集中常量类（项目已有 `RoleEnum`、`StatusEnum`、`ConversationRoleEnum` 等良好范例，应全面对齐）。
  - HTTP / 配置 key、header 名（如 `Snail-Ai-Auth` 已是常量，是好的范例）继续保持常量化。
- 行动项：用 Checkstyle `MagicNumber` 规则 + 人工复核，逐模块清理（排除 0/1/-1 等惯例值与测试）。

---

## 三、日志规范（中优先级）

观察到的问题与目标：

1. **中英文混用**：日志文案中英文混杂（如 `persistAll start` 与「知识库不存在」并存）。建议统一一种语言（推荐英文 key + 结构化字段，或全中文，团队二选一并写入规范）。
2. **级别使用**：
   - 异步持久化失败仅 `log.error` 无补偿（见 task.md #6），日志不能替代可靠性机制。
   - `printStackTrace()` 1 处（`JsonUtil`）、`System.out` 2 处（`QueryDbTool`、`StartListener`）——应替换为 logger。
3. **结构化与脱敏**：
   - 禁止打印敏感信息（token、密码哈希、API Key、密钥）。需专项排查 `log.*` 是否带敏感字段。
   - 推荐占位符 `log.info("... {}", v)`，禁止字符串拼接拼日志。
4. **统一规范**：制定《日志规范》：级别定义（ERROR 需告警 / WARN 可恢复 / INFO 关键链路 / DEBUG 排障）、必带上下文字段（如 `conversationId`、`agentId`、`traceId`）、脱敏要求。

---

## 四、注释与可读性（中 / 低优先级）

- 现状：核心类有类级 Javadoc（`@author`/`@date`），但**方法级「为什么」注释偏少**，部分注释只复述代码。
- 规范目标（与全局规则一致）：
  - 注释只解释**意图 / 取舍 / 约束**，不复述代码；删除「// 创建对象」这类废话注释。
  - 公共 API（对外 service / controller）补 Javadoc：参数含义、异常语义、线程安全性。
  - 复杂条件 / 算法（如 `ChunkMerger`、`SmartChunkStrategy`、`TokenAwareChunker`）补「设计意图」说明。
- 命名：保持现有 `*PO` / `*Service` / `*Facade` / `*VO` 风格一致；新代码遵循 `.claude/skills/dev-guide/naming-conventions.md`。

---

## 五、vendored / 生成代码处理策略

- **vendored 上游代码**（如 `org.springframework.ai.openai.*`）：
  - **不纳入格式化 / 重构范围**，避免与上游同步冲突；
  - 用 Spotless / Checkstyle 的 `excludes` 显式排除该包；
  - 自有增强一律走「子类 / 适配器 / 装饰器」，与原文件隔离，并在文件头标注来源 + 修改点。
- **生成代码**（protobuf gRPC `*.grpc.auto.*`、前端编译产物 `admin/assets`、`chat/assets`）：全部排除出质量门禁。

---

## 六、工具化推进方案（落地核心）

当前零工具，建议**分三阶段引入**，避免一次性大规模 reformat 冲击历史 diff：

### 阶段 1：统一格式（低风险，先行）
- 加 **EditorConfig**（缩进、换行、编码）统一编辑器行为。
- 引入 **Spotless**（`spotless-maven-plugin`）+ Google Java Format 或 Palantir 风格：
  - 先 `spotless:check` 只在 CI 告警、不阻断；
  - 选一个「格式化基线提交」，单独一个 PR 全量 reformat（不混业务改动），便于 review 与 `git blame` 处理（可配 `.git-blame-ignore-revs`）。
  - 排除 vendored / 生成代码目录。

### 阶段 2：静态检查（中风险）
- **Checkstyle**：方法长度、类长度、圈复杂度（`CyclomaticComplexity`）、`MagicNumber`、命名规范、import 顺序。
- **PMD** / **SpotBugs**：空 catch、资源泄漏、可疑 null、并发问题。
- 初期设为 `warn`，统计基线后，**新增代码零新增告警**（增量门禁），存量逐步清。

### 阶段 3：CI 门禁 + 增量治理
- 在 CI（结合 `task.md` #4 取消 `skipTests=true` 一起做）加：`spotless:check` + `checkstyle:check` 阻断；
- 用「增量扫描」（只校验本次 PR 改动文件）实现**只进不退**，避免被存量问题卡死；
- 配 Bugbot / 评审 checklist：超长类 / 长方法 / 魔法值 / 敏感日志四项硬性 review。

### 推荐 Maven 插件骨架（根 pom，示意）
```xml
<plugin>
  <groupId>com.diffplug.spotless</groupId>
  <artifactId>spotless-maven-plugin</artifactId>
  <!-- version 用最新稳定版 -->
  <configuration>
    <java>
      <includes><include>src/main/java/com/aizuda/**/*.java</include></includes>
      <!-- 排除 vendored / 生成代码 -->
      <excludes>
        <exclude>**/org/springframework/ai/**</exclude>
        <exclude>**/grpc/auto/**</exclude>
      </excludes>
      <googleJavaFormat/>
      <removeUnusedImports/>
      <importOrder/>
    </java>
  </configuration>
</plugin>
```

---

## 七、落地路线图（建议顺序）

| 步骤 | 内容 | 风险 | 产出 |
|------|------|------|------|
| 1 | 加 EditorConfig + Spotless（check-only，排除 vendored/生成码） | 低 | 格式基线 |
| 2 | 单独 PR 全量 reformat + `.git-blame-ignore-revs` | 低（独立 PR） | 统一格式 |
| 3 | 制定《日志规范》《注释规范》，清理 `printStackTrace`/`System.out` | 低 | 规范文档 + 小修 |
| 4 | 引入 Checkstyle/PMD（warn 模式 + 基线统计） | 中 | 问题清单 |
| 5 | 拆分 Top 超大类（SkillService 等，逐个独立 PR） | 中 | 单一职责 |
| 6 | CI 增量门禁（新增代码零新增告警 + spotless/checkstyle 阻断） | 中 | 只进不退 |
| 7 | 魔法值收敛到 enum/常量（逐模块） | 中 | 与 dev-guide 对齐 |

> 关键原则：**格式化与业务改动分离 PR**、**增量门禁优先于存量清零**、**vendored/生成码一律排除**。
