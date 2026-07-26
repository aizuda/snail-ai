# Snail AI 安全 / 稳定性 / 测试 整改任务清单

> 复核范围：基于本地源码逐文件核实 GPT 审查报告（10 条），并补充其遗漏项。
> 复核结论：GPT 的 10 条**全部属实**，其中第 8 条实际可利用性低（已确认 `indexName` 为系统生成的 `rag_<Long>`）；第 10 条因缺前端源码无法审计。
> **GPT 遗漏的关键问题已在「二、补充发现」列出（A1~A6 最值得关注）。**
> 备注：项目 `AGENTS.md` 要求使用 GitNexus 图谱分析，但本次会话未暴露相关 MCP 工具，故结论基于源码 / 构建 / 测试静态扫描。

---

## 一、复核 GPT 报告（逐条核实）

| # | 问题 | 核实结论 | 证据 | 严重度 |
|---|------|---------|------|--------|
| 1 | LLM Agent 默认具备 Shell 执行能力 | ✅ 属实 | `ShellTool` 暴露 `/bin/sh -c`、支持链式命令；`BaseToolResolver.resolve` 默认注册 `ShellTool + HttpTool` | 高 |
| 2 | 默认配置含明文 / 固定密钥 | ✅ 属实 | `application.yml` 默认 `root/123456`、crypto key/iv 固定默认值；`ResourceConfig` 默认 MinIO `minioadmin/minioadmin` | 高 |
| 3 | 注册接口开放 + 认证默认放行未标注接口 | ✅ 属实 | `UserController#register` 公开无防护；`AuthenticationInterceptor` 仅校验带 `@LoginRequired` 的方法，漏标注即公开 | 高 |
| 4 | 测试体系几乎空白 | ✅ 属实 | `pom.xml` `<skipTests>true</skipTests>`；全仓库仅 2 个 `*Test.java` | 中 |
| 5 | OpenAPI 流式取消后后台任务不停止 | ✅ 属实 | `OpenApiChatService.chatStream` 用虚拟线程跑 `agentChatService.chat`，`sink.onCancel` 仅 `log.debug`，无中断 | 中 |
| 6 | 异步持久化缺队列 / 重试 / 可观测性 | ✅ 属实 | `ChatResultPersistService.persistAsync` 每次裸起虚拟线程，失败仅 `log.error`，无背压 / 补偿 / 死信 | 中 |
| 7 | RAG 并行检索无超时控制 | ✅ 属实 | `RagHandler.parallelSearch` 每次新建虚拟线程池，对 future 直接 `join()` 无超时 | 中 |
| 8 | PGVector 删除表拼接 SQL 标识符 | ✅ 属实，可利用性低 | `PgSnailAiVectorStore.deleteByIndexName` 拼 `DROP TABLE IF EXISTS + indexName`；但 `indexName` 实为系统生成 `rag_<Long>`，非用户可控 | 低（纵深防御） |
| 9 | Skill ZIP 上传缺资源压力控制 | ✅ 属实 | `unzip` 有 Zip Slip 防护，但 `saveSkillFilesFromZip` 全量 `Files.readString(UTF_8)` 入库，无文件数 / 总大小 / 单文件大小 / 二进制过滤 / zip bomb 限制；二进制按 UTF-8 读取会损坏 | 中 / 低 |
| 10 | 前端源码缺失无法审计 | ✅ 属实 | 仓库仅有编译产物（`admin/assets`），无 `snail-ai-web/` 源码，XSS 等需回前端工程确认 | 待确认 |

---

## 二、补充发现（GPT 遗漏）

### A1. JWT 用「用户密码哈希」作为签名密钥【高】
- `UserService.getToken` 与 `AuthenticationInterceptor.verifyTokenAndGetUser` 均用 `Algorithm.HMAC256(userPO.getPassword())` 签名 / 验签。
- 风险：
  1. JWT 签名密钥 = 数据库里存的 PBKDF2 哈希，一旦库泄露即可离线伪造任意用户 token；
  2. 无统一签名密钥 / 密钥轮换机制；
  3. 验签前先 `JWT.decode` 取 audience 再查库，逻辑可接受但耦合了「认证密钥」与「口令存储」。
- 建议：改用独立的服务端签名密钥（从配置 / KMS 注入），与口令存储解耦；或迁移到标准 Session / 带服务端密钥的 JWT 方案。

### A2. 无注销 / token 失效 / 黑名单机制【中】
- 登录签发 token 后，服务端无任何吊销手段；改密码会因签名密钥变化而间接失效旧 token，但无显式登出、无强制下线、无设备管理。
- 建议：引入服务端会话表或 token 版本号 / jti 黑名单，支持主动登出与强制失效。

### A3. Token 有效期注释与实现不一致，且时长偏长【中 / 低】
- `getToken` 注释写「24小时有效」，实际 `TimeUnit.DAYS.toMillis(7)`（7 天），且无刷新 token 机制。
- 建议：统一注释与实现；缩短 access token 时长 + 引入刷新机制。

### A4. `authorizeUser` 是空操作（功能形同虚设）【中】
- `AuthorizeRequestVO` 带 `totals`（配额）/ `days`（天数），但 `UserService.authorizeUser` 查到用户后直接 `userMapper.updateById(userPO)`，**从未把配额 / 天数写入实体**，日志却打印「授权成功 配额 X 天数 Y」，极具误导性。
- 建议：要么补全配额 / 有效期落库逻辑，要么移除该无效接口，避免管理员误以为已生效。

### A5. `generateCode` 使用 `java.util.Random`（非安全随机）【低】
- `UserService.generateCode` 用 `new Random()` 生成「JWT 签名密钥」字符串；当前签名实际改用了密码哈希，该方法**疑似已成死代码**。
- 建议：删除死代码；如确需随机串，统一用 `SecureRandom`（项目已有 `SECURE_RANDOM` 常量）。

### A6. HttpTool 存在 DNS 重绑定 TOCTOU【中】
- `HttpTool.validatePublicHttpTarget` 已对 loopback / 内网 / link-local / 唯一本地 IPv6 / `localhost` 做了拦截（GPT 把它当「裸 SSRF」并不准确，防护其实存在，且 `followRedirects(NEVER)`）。
- 但缺陷在于：校验时 `InetAddress.getAllByName` 解析一次，随后 `httpClient.send` 又会**重新解析**，攻击者可用 DNS 重绑定在两次解析间切换到内网 IP，绕过校验（TOCTOU）。
- 建议：解析后锁定已校验 IP 直连（自定义 DNS resolver / 连接到已校验地址并带 SNI/Host），或使用 SSRF 安全的 HTTP 客户端。

### A7. `/files/**` 完全绕过登录鉴权【低 / 中（需确认）】
- `SnailJobWebMvcConfigurerAdapter` 对认证拦截器 `excludePathPatterns("/openapi/**", "/files/**")`。
- `/openapi/**` 有独立的 `OpenApiAuthInterceptor`，合理。
- `/files/**`（`FileAccessController`）为「公开资源访问入口」，靠 **UUID storageKey 正则 + 路径穿越防护（`resolveAbsolute` startsWith 校验）** 保护，设计上属于「凭 URL 即可访问」（capability 模式），头像等场景可接受。
- 风险点：无逐资源鉴权，若后续把敏感文件（如 RAG 私有文档）也走该路径，等于公开。
- 建议：明确区分公开 / 私有资源；私有资源走带鉴权的下载通道。

### A8. 登录 / 注册无限流与防爆破【中】
- `login` / `register` 无验证码、无失败锁定、无 IP / 账号级限流，可被撞库 / 枚举（注册还能枚举已存在账号：「账号已存在」）。
- 建议：加图形 / 行为验证码、失败次数锁定、限流；注册改邀请码或开关控制。

### A9. 全局异常返回过于笼统【低】
- `RestExceptionHandler.onException` 对所有 `Exception` 统一返回 `code=0, "System exception"`（**无堆栈泄露，这点是好的**），但所有错误都用业务码 0、HTTP 200，缺少 4xx/5xx 区分，前端与监控难以分级告警。
- 建议：保留不泄露内部信息的前提下，按异常类型映射合适的 HTTP 状态码 / 业务码。

### A10. 缺少全局 CORS 显式配置【低 / 待确认】
- 未在 admin 模块发现 `addCorsMappings` / `CorsConfiguration`，需确认是否依赖网关或同源策略，避免线上 CORS 配置缺失或过宽。

---

## 三、整改优先级与建议顺序

### P0（高危，优先处理）
- [ ] **A1**：JWT 改用独立服务端签名密钥，与口令存储解耦
- [ ] **#1**：`ShellTool` 改为显式开关 + 命令白名单 + 权限隔离 + 审计；默认关闭
- [ ] **#2**：敏感配置（DB / crypto key、iv / MinIO）改为环境变量注入，并在启动时强校验「不得为默认值」
- [ ] **#3 + A8**：注册改默认关闭 / 邀请码 + 限流；认证拦截器改「默认鉴权、显式放行」

### P1（中危）
- [ ] **A2 / A3**：引入 token 吊销 / 登出机制；统一并缩短有效期 + 刷新机制
- [ ] **A4**：修复 `authorizeUser` 配额 / 天数落库（或移除无效接口）
- [ ] **A6**：修复 HttpTool DNS 重绑定 TOCTOU
- [ ] **#5**：OpenAPI 流式 `onCancel` 真正中断后台虚拟线程 / 传播取消信号
- [ ] **#6**：持久化引入队列 + 重试 + 失败补偿 + 监控指标
- [ ] **#7**：RAG 并行检索加单库超时 + 整体超时 + 复用线程池
- [ ] **#9**：Skill 上传加文件数 / 总大小 / 单文件大小 / 二进制识别 / zip bomb 防护
- [ ] **A7**：明确公开 / 私有资源边界，私有资源走鉴权下载

### P2（低危 / 工程质量）
- [ ] **#4**：补核心链路测试（登录鉴权、Agent 调度、RAG、Skill 上传、OpenAPI、资源访问），并取消全局 `skipTests=true`
- [ ] **#8 / A5**：PGVector 标识符校验 / 白名单（纵深防御）；删除 `generateCode` 死代码或改 `SecureRandom`
- [ ] **A9 / A10**：异常按类型映射 HTTP 状态码；确认 / 补全 CORS 配置
- [ ] **#10**：在前端源码工程内审计 XSS（`innerHTML` / Markdown 渲染）

---

## 四、升级兼容性影响矩阵

> 破坏等级图例：🟢 无影响 · 🟡 需重登 · 🟠 需配置 / 行为变更 · 🔴 需迁移脚本 / 数据风险

| 整改项 | 破坏等级 | 升级时影响 | 兼容措施 |
|--------|---------|-----------|---------|
| #2 默认密钥强校验（crypto key/iv） | 🔴 数据风险 | 强制换 key 会导致**旧加密数据（模型 API Key 等）无法解密**；fail-fast 会让用旧默认值的存量部署**起不来** | crypto key 绝不静默轮换；要换必须配「旧 key 解密→新 key 重加密」迁移脚本；存量安装用 WARN 不阻断，或加 `allow-default-secrets` 开关默认兼容 |
| #2 默认密钥强校验（DB / MinIO） | 🟠 需配置 | 若 fail-fast，用默认 `root/123456`、`minioadmin` 的部署启动失败 | 改环境变量注入；存量用 WARN，新装强制；文档引导 |
| A1 JWT 签名密钥独立 | 🟡 需重登 | 切换瞬间**所有在线 token 失效，全员强制登出**；一次性、无数据丢失 | 发版说明提前公告；可选灰度（双密钥并行验签一段时间） |
| 口令哈希 PBKDF2 | 🟢 无影响 | 已有平滑迁移：登录时旧 SHA-256 自动升级 PBKDF2 | 无需额外动作 |
| #3 认证默认拒绝 | 🟠 行为变更 | 原来漏标 `@LoginRequired` 的**隐式公开接口会突然要求登录**，可能 401 打断前端 | 落地前先审计「无注解但被匿名访问」接口，建显式放行白名单 |
| #3 注册关闭 / 限流 | 🟠 行为变更 | 自助注册被关 / 被限，产品行为变化 | 用开关控制，确认现网依赖后再切默认 |
| #1 ShellTool 默认关闭 + 白名单 | 🟠 行为变更 | 现存依赖 shell 工具的 Agent 升级后失效 | 提供 `enabled` 开关 + 命令白名单配置 + 迁移说明 |
| A7 `/files/**` 加鉴权 | 🟠 行为变更 | 已分发的**公开 URL（头像 / 内嵌图）会 403** | 不给现有公开资源加鉴权；区分公开 / 私有，私有走鉴权下载通道 |
| A4 authorizeUser 落库 | 🔴 需迁移脚本 | 现在本就没生效，补全几乎不破坏旧行为，但需加配额 / 天数字段 | 一次 DB schema 迁移（加列，带默认值） |
| A2 token 吊销 / 黑名单 | 🟢 无影响 | 增量能力 | 可选加会话表 / jti 黑名单 |
| A6 HttpTool TOCTOU 修复 | 🟢 无影响 | 内部实现 | 无需用户动作 |
| #5 流式取消传播 | 🟢 无影响 | 内部实现 | 无需用户动作 |
| #6 持久化队列 / 重试 | 🟢 无影响 | 内部实现 | 无需用户动作 |
| #7 RAG 超时控制 | 🟢 无影响 | 内部实现，仅慢查询行为变化 | 超时阈值可配置 |
| #9 Skill 上传限制 | 🟢 无影响 | 仅约束**新上传**，存量 Skill 不受影响 | 限额可配置 |
| #8 / A5 PG 标识符校验 / 删除死代码 | 🟢 无影响 | 内部实现 | 无需用户动作 |
| #4 取消 skipTests / 补测试 | 🟢 无影响 | 仅构建期 | 无需用户动作 |

**安全升级总策略**：① 所有收紧项挂配置开关，默认保持兼容老行为，分版本逐步切默认；② crypto / DB 等密钥类只告警不阻断，并提供 re-encrypt 迁移脚本；③ 认证默认拒绝前先产出「隐式公开接口」白名单；④ 发版说明明确列出「需重新登录 / 需配置项 / 需执行迁移脚本」三类动作。

---

## 五、本次未覆盖 / 需后续确认
- 前端 Vue/TS 源码级安全（XSS、token 存储位置、越权）——需回 `snail-ai-web/` 工程。
- GitNexus 图谱级影响面分析（本会话工具未就绪）。
- 生产部署形态下网关 / WAF / 限流是否已在外层兜底。
