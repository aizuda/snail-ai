---
name: snail-ai-dev-guide
description: Snail AI 项目开发规范和最佳实践指南。提供代码风格、命名规范、架构设计、数据库操作等全方位的开发指导。
license: MIT
homepage: https://github.com/aizuda/snail-ai
metadata:
  version: "1.0.0"
  author: "opensnail"
  tags: ["development", "coding-standards", "best-practices", "java", "spring-boot"]
---

# Snail AI 开发规范指南

欢迎使用 Snail AI 开发规范 Skill！本指南提供完整的项目开发规范、最佳实践和代码示例。

## 概述

**Snail AI** 是一个基于 Spring Boot 和 Spring AI 的智能对话应用平台,采用模块化架构设计。

### 技术栈

- **Java**: 17/21
- **Spring Boot**: 4.0.1
- **Spring AI**: 2.0.0-M4
- **MyBatis-Plus**: 3.5.16
- **数据库**: MySQL/PostgreSQL
- **缓存**: Redis (Redisson 4.1.0)
- **向量数据库**: Milvus 2.4.8
- **搜索引擎**: Elasticsearch 8.17.0
- **工具库**: Hutool 5.8.38, Lombok 1.18.42
- **AI 集成**: DeepSeek, OpenAI, LangChain4j 0.34.0

## 快速开始

### 如何使用本 Skill

当你需要了解开发规范时，可以：

1. **查询命名规范**: "我要创建一个用户查询VO,应该怎么命名?"
2. **查看代码示例**: "给我一个标准的Controller示例"
3. **架构咨询**: "这个功能应该放在哪个模块?"
4. **代码审查**: "帮我审查这段代码是否符合规范"

### 文档索引

本 Skill 包含以下详细文档：

- **architecture.md** - 架构设计和模块划分指南
- **coding-standards.md** - 编码规范详解（命名、注解、注释）
- **database-guide.md** - 数据库和持久层操作指南
- **api-design.md** - RESTful API 设计规范
- **naming-conventions.md** - 命名规范速查表
- **common-patterns.md** - 常用设计模式和最佳实践
- **examples/** - 完整的代码示例

## 核心开发原则

### 1. 禁止魔法值

❌ **不推荐**:
```java
if (status.equals(rawStatus)) {
    // ...
}
```

✅ **推荐**:
```java
// 使用枚举
if (status == MemoryStatusEnum.ACTIVE) {
    // ...
}

// 或使用常量
if (status.equals(StatusConstants.ACTIVE)) {
    // ...
}
```

### 2. 方法参数超过 3 个使用对象封装

❌ **不推荐**:
```java
public void createMemory(Long agentId, Long userId, String content, 
                        String type, BigDecimal score, LocalDateTime time) {
    // ...
}
```

✅ **推荐**:
```java
public void createMemory(CreateMemoryRequest request) {
    // request 包含所有参数
}
```

### 3. 减少 if 复杂度

**使用卫语句/提前返回**:
```java
// 不推荐
public void process(User user) {
    if (user != null) {
        if (user.isActive()) {
            if (user.hasPermission()) {
                // 业务逻辑
            }
        }
    }
}

// 推荐
public void process(User user) {
    if (user == null) return;
    if (!user.isActive()) return;
    if (!user.hasPermission()) return;
    
    // 业务逻辑
}
```

**Hutool 优先，Optional 只用于链式转换**:
```java
if (StrUtil.isBlank(userName)) {
    return;
}

Optional.ofNullable(userMapper.selectById(id))
    .map(UserPO::getEmail)
    .ifPresent(this::sendEmail);
```

### 4. 谨慎使用设计模式

只在真正需要时使用设计模式，避免过度设计：

- **单一职责（SRP）**: 每个类只做一类事
- **策略模式**: 多种可替换算法（如多种 AI 模型）
- **工厂模式**: 按类型创建实现（如 Tool 工具创建）
- **建造者模式**: 多可选参数构建（使用 Lombok @Builder）
- **门面模式**: 统一对外入口（Service 层封装）
- **模板方法**: 固定流程、部分步骤由子类实现

## 项目模块架构

Snail AI 采用多模块 Maven 架构:

```
snail-ai/
├── snail-ai-common          # 通用工具和基础类
├── snail-ai-persistence     # 持久层（Mapper、PO）
├── snail-ai-model          # AI 模型集成
├── snail-ai-memory         # 记忆系统
├── snail-ai-features       # 功能特性（RAG、Skill、Tool）
├── snail-ai-admin          # 管理后端（Controller、Service、VO）
└── snail-ai-starter        # 启动模块
```

### 模块依赖关系

```
snail-ai-starter
    └── snail-ai-admin
        ├── snail-ai-features
        │   ├── snail-ai-model
        │   ├── snail-ai-memory
        │   └── snail-ai-persistence
        ├── snail-ai-memory
        │   └── snail-ai-persistence
        └── snail-ai-persistence
            └── snail-ai-common
```

**依赖原则**:
- 上层模块可以依赖下层模块
- 同层模块之间避免循环依赖
- common 模块不依赖任何其他模块

## 包结构规范

标准包结构：

```
com.aizuda.snail.ai.{module}/
├── controller/          # Web 控制器
├── service/            # 业务服务层
├── dto/                # 数据传输对象（跨模块）
├── vo/                 # 视图对象（API 响应）
│   ├── {feature}/      # 按功能分类
├── po/                 # 持久化对象（对应数据库表）
├── mapper/             # MyBatis Plus 数据访问层
├── enums/              # 枚举类型
├── constant/           # 常量定义
├── handler/            # 事件处理器、异常处理
├── config/             # 配置类
├── interceptor/        # 拦截器
├── security/           # 安全相关
└── util/               # 工具类
```

## 命名规范概览

### 类命名

| 类型 | 命名规则 | 示例 |
|-----|---------|------|
| VO 类 | `{Name}VO` | `UserInfoVO`, `MemoryStatsVO` |
| DTO 类 | `{Name}DTO` | `AudienceDTO`, `ChatCompletionDTO` |
| PO 类 | `{Name}PO` | `ConversationMemoryPO`, `UserPO` |
| Mapper | `{Name}Mapper` | `ConversationMemoryMapper` |
| Service ⚠️ | `{Name}Service` | `UserService` (仅 admin 模块) |
| Handler ⚠️ | `{Name}Handler` | `MemoryRetriever` (其他模块) |
| Controller | `{Name}Controller` | `AgentController` |
| Enum | `{Name}Enum` | `RoleEnum`, `MemoryStatusEnum` |
| Exception | `{Name}Exception` | `SnailAiCommonException` |
| Constants | `{Name}Constants` | `CommonConstants` |

**重要**: Service 后缀仅用于 admin 模块,其他模块使用 Handler 后缀或特定名称

### 方法命名

| 操作类型 | 前缀 | 示例 |
|---------|-----|------|
| 查询 | `get`, `query`, `fetch`, `retrieve` | `getUserInfo()`, `queryMemories()` |
| 创建 | `create`, `add` | `createMemory()`, `addUser()` |
| 更新 | `update`, `set`, `modify` | `updateTitle()`, `setStatus()` |
| 删除 | `delete`, `remove` | `deleteMemory()`, `removeUser()` |
| 判断 | `is`, `has`, `check` | `isActive()`, `hasPermission()` |
| 业务操作 | 具体动词 | `login()`, `authorize()`, `archive()` |

### 字段命名

| 字段类型 | 命名规则 | 示例 |
|---------|---------|------|
| Boolean | `is` 前缀或现在分词 | `isActive`, `autoExtract` |
| 时间 | 后缀 `Dt` | `createDt`, `updateDt` |
| ID | 后缀 `Id` | `userId`, `agentId` |
| 状态/类型 | 枚举或英文名 | `memoryType`, `status` |
| 计数 | 后缀 `Count` | `accessCount` |
| 评分 | 后缀 `Score` | `relevanceScore` |

## 常用注解

### Spring 注解
```java
@RestController                    // Web 控制器
@RequestMapping("/api/users")     // 路由映射
@Service                          // 服务层
@GetMapping("/{id}")              // GET 请求
@PostMapping                      // POST 请求
@PutMapping("/{id}")              // PUT 请求
@DeleteMapping("/{id}")           // DELETE 请求
@PathVariable Long id             // 路径变量
@RequestParam String keyword      // 请求参数
@RequestBody UserVO vo            // 请求体
@Validated                        // 参数验证
```

### Lombok 注解
```java
@Data                             // getter/setter/toString/equals/hashCode
@Builder                          // Builder 模式
@AllArgsConstructor               // 全参构造
@NoArgsConstructor                // 无参构造
@RequiredArgsConstructor          // 必需字段构造（用于依赖注入）
@Slf4j                            // 日志
```

### MyBatis-Plus 注解
```java
@TableName("snail_ai_user")       // 表名映射
@TableId(type = IdType.AUTO)      // 自增主键
@EnumValue                         // 枚举值映射
```

### 自定义注解
```java
@LoginRequired                     // 需要登录
@LoginRequired(role = RoleEnum.ADMIN)  // 需要管理员权限
```

## 异常处理

### 异常体系

```java
BaseSnailAiException                           // 基类
├── SnailAiCommonException                    // 通用业务异常
├── SnailAiAuthenticationException (5001)     // 认证异常
├── SnailAiAiException                        // AI 相关异常
├── ModelCallException                        // 模型调用异常
└── 特定模块异常
    ├── SearchEngineException                 // 搜索引擎异常
    └── VectorStoreException                  // 向量库异常
```

### 使用示例

```java
// 抛出异常
throw new SnailAiCommonException("用户不存在");
throw new SnailAiAuthenticationException("认证失败");

// 支持格式化参数
throw new SnailAiCommonException("用户 {} 不存在", username);
```

## 日志规范

使用 Lombok 的 `@Slf4j` 注解:

```java
@Slf4j
@Service
public class UserService {
    
    public void register(LoginRequestVO requestVO) {
        // 业务逻辑
        log.info("新用户注册成功: {}", requestVO.getUsername());
    }
    
    public void handleError(Exception e) {
        log.error("处理失败", e);
    }
}
```

**日志级别**:
- `info`: 重要业务操作（登录、注册、更新等）
- `error`: 异常情况
- `debug`: 调试信息（生产环境通常关闭）
- `warn`: 警告信息

## 数据库规范

### 表设计规范

- 主键使用 `id` (Long 类型，自增)
- 时间戳使用 `LocalDateTime`，字段名后缀为 `Dt`
- 关键字段添加索引（如 `relevance_score`、`accessed_at`）
- 使用枚举存储状态值

### PO 类示例

```java
@TableName("snail_ai_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    private String email;
    
    @EnumValue
    private RoleEnum role;
    
    private LocalDateTime createDt;
    private LocalDateTime updateDt;
}
```

### Mapper 接口

```java
public interface UserMapper extends BaseMapper<UserPO> {
    // 继承 BaseMapper 自动获得 CRUD 功能
    // 复杂查询使用 LambdaQueryWrapper
}
```

### Service 层查询

```java
// Lambda 查询
LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<UserPO>()
    .eq(UserPO::getUsername, username)
    .eq(UserPO::getStatus, StatusEnum.ACTIVE);
UserPO user = userMapper.selectOne(wrapper);

// 分页查询
PageDTO<UserPO> pageDTO = new PageDTO<>(page, size);
PageDTO<UserPO> result = userMapper.selectPage(pageDTO, wrapper);
```

## API 设计规范

### 统一响应格式

```java
// 单个对象
Result<UserVO> result = Result.ok(userVO);

// 分页数据
PageResult<List<UserVO>> pageResult = new PageResult<>(total, list);

// 失败响应
Result<String> result = Result.fail("操作失败");
```

### Controller 标准写法

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    @LoginRequired
    public Result<UserVO> getUser(@PathVariable Long id) {
        return Result.ok(userService.getUser(id));
    }
    
    @PostMapping
    @LoginRequired
    public Result<UserVO> createUser(@RequestBody @Validated UserCreateVO vo) {
        return Result.ok(userService.createUser(vo));
    }
    
    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<UserVO>> page(UserQueryVO queryVO) {
        return userService.page(queryVO);
    }
}
```

## 参考文档

- **详细规范**: 查看 `coding-standards.md`
- **架构设计**: 查看 `architecture.md`
- **数据库指南**: 查看 `database-guide.md`
- **API 设计**: 查看 `api-design.md`
- **命名速查**: 查看 `naming-conventions.md`
- **设计模式**: 查看 `common-patterns.md`
- **代码示例**: 查看 `examples/` 目录

## 项目文档

项目根目录的重要文档：

- `/docs/CODE_STYLE.md` - 完整的代码规范文档
- `/MEMORY_QUICK_START.md` - 记忆系统快速开始
- `/记忆系统实现总结.md` - 记忆系统实现详细总结

## 获取帮助

如果你需要：
- 查看具体的代码示例
- 了解某个模块的详细设计
- 审查代码是否符合规范
- 解决具体的技术问题

请直接提问，我会根据本规范为你提供详细的指导和建议。

---

**版本**: 1.0.0  
**作者**: opensnail  
**最后更新**: 2026-04-01
