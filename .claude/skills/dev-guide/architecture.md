# Snail AI 架构设计指南

本文档详细说明 Snail AI 项目的架构设计、模块划分和依赖关系。

## 目录

- [整体架构](#整体架构)
- [模块职责](#模块职责)
- [分层架构](#分层架构)
- [包结构规范](#包结构规范)
- [模块间通信](#模块间通信)
- [依赖注入](#依赖注入)

## 整体架构

Snail AI 采用**模块化微服务风格架构**,将系统划分为 7 个核心模块:

```
snail-ai/
├── snail-ai-common          # 通用基础模块
├── snail-ai-persistence     # 数据持久化模块
├── snail-ai-model          # AI 模型集成模块
├── snail-ai-memory         # 记忆管理模块
├── snail-ai-features       # 功能特性模块
├── snail-ai-admin          # 管理服务模块
└── snail-ai-starter        # 应用启动模块
```

### 架构设计原则

1. **模块化**: 每个模块有明确的职责边界
2. **分层清晰**: 上层依赖下层,同层模块避免循环依赖
3. **低耦合高内聚**: 模块间通过接口通信
4. **可扩展性**: 便于添加新功能和模块
5. **可维护性**: 代码组织清晰,易于理解和修改

## 模块职责

### 1. snail-ai-common (通用模块)

**职责**: 提供全局通用的基础设施和工具类

**包含内容**:
- **enums**: 全局枚举（StatusEnum、RoleEnum 等）
- **constant**: 全局常量定义
- **exception**: 异常体系（BaseSnailAiException、SnailAiCommonException 等）
- **model**: 通用数据模型（Result、PageResult 等）
- **util**: 工具类（JsonUtil、DateUtil 等）
- **vo**: 通用 VO（BaseQueryVO 等）

**依赖关系**: 不依赖任何其他模块

**示例代码位置**:
- `/snail-ai-common/src/main/java/com/aizuda/snail/ai/common/enums/`
- `/snail-ai-common/src/main/java/com/aizuda/snail/ai/common/exception/`
- `/snail-ai-common/src/main/java/com/aizuda/snail/ai/common/model/`

### 2. snail-ai-persistence (持久化模块)

**职责**: 封装所有数据访问逻辑,提供统一的数据访问接口

**包含内容**:
- **po**: 持久化对象（对应数据库表）
  - `admin/po/` - 用户、权限等
  - `memory/po/` - 记忆相关表
  - `skill/po/` - 技能相关表
  - `rag/po/` - RAG 文档和分块表
- **mapper**: MyBatis-Plus Mapper 接口
  - 继承 `BaseMapper<PO>`
  - 无需 XML 配置

**依赖关系**: 
- 依赖 `snail-ai-common`
- 被 `snail-ai-memory`、`snail-ai-features`、`snail-ai-admin` 依赖

**示例代码位置**:
- `/snail-ai-persistence/src/main/java/com/aizuda/snail/ai/persistence/memory/po/ConversationMemoryPO.java`
- `/snail-ai-persistence/src/main/java/com/aizuda/snail/ai/persistence/memory/mapper/ConversationMemoryMapper.java`

### 3. snail-ai-model (AI 模型模块)

**职责**: 封装 AI 模型调用,提供统一的模型访问接口

**包含内容**:
- **client**: AI 模型客户端封装
- **dto**: 模型请求/响应 DTO
- **config**: 模型配置
- **service**: 模型调用服务

**支持的模型**:
- DeepSeek
- OpenAI
- 火山引擎 Ark

**依赖关系**:
- 依赖 `snail-ai-common`
- 依赖 Spring AI 框架
- 被 `snail-ai-features` 依赖

**示例代码位置**:
- `/snail-ai-model/src/main/java/com/aizuda/snail/ai/model/`

### 4. snail-ai-memory (记忆模块)

**职责**: 管理对话记忆、上下文和记忆检索

**包含内容**:
- **service**: （⚠️ 使用 Handler 后缀或特定名称）
  - `MemoryExtractionService` - 记忆提取
  - `MemoryRetriever` - 记忆检索
  - `ContextManager` - 上下文管理
- **dto**: 记忆相关 DTO
- **enums**: 记忆类型枚举（FACT、DECISION、PREFERENCE 等）

**核心功能**:
- 自动提取对话中的关键信息
- 向量化存储和检索记忆
- 记忆压缩和归档
- 多维度记忆检索（语义、时间、类型）

**依赖关系**:
- 依赖 `snail-ai-persistence`（访问记忆表）
- 依赖 `snail-ai-common`
- 被 `snail-ai-features`、`snail-ai-admin` 依赖

**示例代码位置**:
- `/snail-ai-memory/src/main/java/com/aizuda/snail/ai/memory/service/MemoryRetriever.java`
- `/snail-ai-memory/src/main/java/com/aizuda/snail/ai/memory/dto/ConversationMemoryDTO.java`

### 5. snail-ai-features (功能特性模块)

**职责**: 实现具体的业务功能特性

**包含内容**:
- **skill**: 技能系统（⚠️ 使用 Handler 后缀）
  - `service/SkillService` - 技能管理接口（features层）
  - `tool/ReadSkillTool` - 读取技能工具
  - `tool/ShellTool` - Shell 执行工具
  - `tool/HttpTool` - HTTP 请求工具
- **rag**: RAG (检索增强生成)（⚠️ 使用 Handler 后缀）
  - `service/RagService` - RAG 服务接口
  - `parser/` - 文档解析器（PDF、Word、Excel）
  - `chunker/` - 文本分块
  - `vectorstore/` - 向量存储（Milvus）
- **knowledge**: 知识库管理
- **search**: 搜索引擎集成（Elasticsearch）

**依赖关系**:
- 依赖 `snail-ai-persistence`
- 依赖 `snail-ai-memory`
- 依赖 `snail-ai-model`
- 被 `snail-ai-admin` 依赖

**示例代码位置**:
- `/snail-ai-features/src/main/java/com/aizuda/snail/ai/features/skill/`
- `/snail-ai-features/src/main/java/com/aizuda/snail/ai/features/rag/`

### 6. snail-ai-admin (管理服务模块)

**职责**: 提供 RESTful API,对外暴露管理接口

**包含内容**:
- **controller**: Web 控制器
  - `UserController`, `AgentController`, `MemoryController` 等
- **service**: 业务服务层（⚠️ 使用 Service 后缀）
  - `UserService`, `MemoryService`, `SkillService` 等
  - 实现具体业务逻辑
  - 调用 features 和 memory 模块
- **vo**: 视图对象
  - 按功能分类（`memory/`, `skill/`, `rag/`, `agent/` 等）
  - 包含 RequestVO、ResponseVO、QueryVO
- **security**: 安全相关
  - `@LoginRequired` 注解
  - JWT 认证
  - 权限控制
- **handler**: 异常处理器
  - `RestExceptionHandler` - 全局异常处理

**依赖关系**:
- 依赖所有下层模块
- 是对外服务的入口

**示例代码位置**:
- `/snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/controller/MemoryController.java`
- `/snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/service/memory/MemoryService.java`
- `/snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/vo/memory/`

### 7. snail-ai-starter (启动模块)

**职责**: Spring Boot 应用启动入口

**包含内容**:
- **SnailAiSpringbootApplication**: 主启动类
- **application.yml**: 应用配置
- **数据库驱动**: MySQL/PostgreSQL

**配置**:
```java
@ComponentScan(basePackages = {
    "com.aizuda.snail.ai.admin",
    "com.aizuda.snail.ai.features",
    "com.aizuda.snail.ai.memory",
    "com.aizuda.snail.ai.model",
    "com.aizuda.snail.ai.persistence",
    "com.aizuda.snail.ai.common"
})
@MapperScan("com.aizuda.snail.ai.persistence")
@EnableAsync
```

**依赖关系**:
- 依赖 `snail-ai-admin`（传递依赖所有模块）

## 分层架构

### 三层架构模式

```
┌─────────────────────────────────────┐
│      Controller Layer (Web层)       │  ← RESTful API 入口
│  UserController, MemoryController   │
└──────────────┬──────────────────────┘
               │ 调用
               ↓
┌─────────────────────────────────────┐
│      Service Layer (业务层)          │  ← 业务逻辑处理
│   UserService, MemoryService        │
└──────────────┬──────────────────────┘
               │ 调用
               ↓
┌─────────────────────────────────────┐
│   Persistence Layer (持久层)        │  ← 数据访问
│  UserMapper, ConversationMemoryMapper│
└──────────────┬──────────────────────┘
               │ 访问
               ↓
┌─────────────────────────────────────┐
│         Database (数据库)            │
│      MySQL / PostgreSQL             │
└─────────────────────────────────────┘
```

### 数据流向

**请求流程**:
1. **Client** → HTTP Request
2. **Controller** → 接收请求,参数验证
3. **Service** → 业务逻辑处理
4. **Mapper** → 数据库操作
5. **Database** → 返回数据
6. **Service** → 数据转换和业务处理
7. **Controller** → 返回 Response

**VO/DTO/PO 转换**:
```
Client Request
    ↓
RequestVO (Controller 接收)
    ↓
DTO (Service 层处理)
    ↓
PO (Mapper 层持久化)
    ↓
Database
    ↓
PO (从数据库查询)
    ↓
DTO (Service 层转换)
    ↓
ResponseVO (Controller 返回)
    ↓
Client Response
```

## 包结构规范

### 标准包结构

每个模块的包结构遵循统一的规范:

```
com.aizuda.snail.ai.{module}/
├── config/                 # 配置类
│   └── {Module}Config.java
├── constant/               # 常量定义
│   └── {Module}Constants.java
├── controller/             # Web 控制器 (仅 admin 模块)
│   ├── UserController.java
│   └── MemoryController.java
├── service/                # 业务服务
│   ├── UserService.java
│   └── impl/
│       └── UserServiceImpl.java
├── dto/                    # 数据传输对象
│   ├── UserDTO.java
│   └── request/
│       └── CreateUserRequest.java
├── vo/                     # 视图对象 (仅 admin 模块)
│   ├── user/
│   │   ├── UserResponseVO.java
│   │   └── UserQueryVO.java
│   └── memory/
│       ├── MemoryResponseVO.java
│       └── MemoryQueryVO.java
├── po/                     # 持久化对象 (仅 persistence 模块)
│   └── UserPO.java
├── mapper/                 # MyBatis Mapper (仅 persistence 模块)
│   └── UserMapper.java
├── enums/                  # 枚举类型
│   ├── RoleEnum.java
│   └── StatusEnum.java
├── exception/              # 异常定义
│   └── {Module}Exception.java
├── handler/                # 处理器
│   ├── RestExceptionHandler.java
│   └── EventHandler.java
├── interceptor/            # 拦截器
│   └── AuthInterceptor.java
├── security/               # 安全相关
│   ├── annotation/
│   │   └── LoginRequired.java
│   └── UserSessionUtils.java
├── util/                   # 工具类
│   └── {Module}Util.java
└── tool/                   # 工具（LangChain4j Tool）
    ├── ReadSkillTool.java
    └── ShellTool.java
```

### 包命名规则

| 包名 | 用途 | 命名规范 |
|-----|------|---------|
| `config` | 配置类 | 单数形式 |
| `constant` | 常量 | 单数形式 |
| `controller` | 控制器 | 单数形式 |
| `service` | 服务 | 单数形式 |
| `dto` | 数据传输对象 | 单数形式 |
| `vo` | 视图对象 | 单数形式,按功能分子包 |
| `po` | 持久化对象 | 单数形式,按模块分子包 |
| `mapper` | 数据访问 | 单数形式 |
| `enums` | 枚举 | 复数形式 |
| `util` | 工具类 | 单数形式 |

## 模块间通信

### 依赖方向规则

```
[上层模块] 可以依赖 [下层模块]
[下层模块] 不能依赖 [上层模块]
[同层模块] 避免循环依赖
```

### 正确的依赖示例

✅ **允许**:
```
admin → features → persistence
admin → memory → persistence
features → model
```

❌ **禁止**:
```
persistence → admin  (下层依赖上层)
memory → features    (同层循环依赖)
```

### 接口设计原则

**Features 模块向外提供接口**:
```java
// snail-ai-features/skill/service/SkillService.java
public interface SkillService {
    String loadSkillFilesToTempDir(Long skillId);
    List<SkillPO> getSkillsWithContentForAgent(Long agentId);
    String buildSkillsList(List<SkillPO> skills);
}
```

**Admin 模块实现接口并扩展**:
```java
// snail-ai-admin/service/skill/SkillService.java
@Service
public class SkillService implements com.aizuda.snail.ai.features.skill.service.SkillService {
    
    // 实现 features 接口
    @Override
    public String loadSkillFilesToTempDir(Long skillId) {
        // 实现
    }
    
    // 扩展管理功能
    public SkillResponseVO upload(MultipartFile file) {
        // admin 特有功能
    }
    
    public PageResult<List<SkillResponseVO>> page(int page, int size, String keyword) {
        // admin 特有功能
    }
}
```

## 依赖注入

### 使用 Lombok @RequiredArgsConstructor

**推荐方式** (构造函数注入):

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor  // Lombok 自动生成构造函数
public class UserController {
    
    private final UserService userService;  // final 字段会被注入
    private final MemoryService memoryService;
    
    // 无需写构造函数,Lombok 自动生成
}
```

**等价于**:
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    private final MemoryService memoryService;
    
    @Autowired  // Spring 4.3+ 可省略
    public UserController(UserService userService, MemoryService memoryService) {
        this.userService = userService;
        this.memoryService = memoryService;
    }
}
```

### 为什么使用构造函数注入

✅ **优点**:
1. **不可变性**: 使用 `final` 字段,保证线程安全
2. **强制依赖**: 构造时必须提供依赖,避免 NPE
3. **易于测试**: 可以直接 new 对象进行测试
4. **循环依赖检测**: 编译时就能发现循环依赖

❌ **避免使用字段注入**:
```java
// 不推荐
@Autowired
private UserService userService;  // 字段不是 final,可能为 null
```

## 配置管理

### application.yml 组织

```yaml
spring:
  application:
    name: snail-ai
  
  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:3306/snail_ai
    username: root
    password: ${DB_PASSWORD}
  
  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379

# MyBatis-Plus 配置
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

# 自定义配置
snail-ai:
  memory:
    auto-extract: true
    max-context-length: 10
  skill:
    storage-path: upload/skills/
```

### 配置类注入

```java
@Configuration
@ConfigurationProperties(prefix = "snail-ai.memory")
@Data
public class MemoryConfig {
    private Boolean autoExtract = true;
    private Integer maxContextLength = 10;
    private Integer extractThreshold = 5;
}
```

## 实际案例

### 案例 1: 创建新功能模块

**需求**: 添加"标签管理"功能

**步骤**:
1. **Persistence 层** - 创建 PO 和 Mapper
2. **Features 层** - 创建 Service 接口
3. **Admin 层** - 实现 Service,创建 Controller 和 VO
4. **更新依赖** - 确保依赖关系正确

### 案例 2: 跨模块调用

**场景**: MemoryController 需要调用 SkillService

```java
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {
    
    private final MemoryService memoryService;
    private final SkillService skillService;  // 跨模块调用
    
    @GetMapping("/with-skills/{agentId}")
    public Result<MemoryWithSkillsVO> getMemoryWithSkills(@PathVariable Long agentId) {
        List<MemoryDTO> memories = memoryService.getMemories(agentId);
        List<SkillPO> skills = skillService.getSkillsWithContentForAgent(agentId);
        
        return Result.ok(new MemoryWithSkillsVO(memories, skills));
    }
}
```

## 总结

Snail AI 的架构设计遵循以下核心原则:

1. **模块化**: 7 个模块,职责清晰
2. **分层架构**: Controller → Service → Mapper → Database
3. **依赖单向**: 上层依赖下层,避免循环
4. **接口抽象**: Features 提供接口,Admin 实现扩展
5. **包结构统一**: 所有模块遵循相同的包结构规范
6. **构造函数注入**: 使用 @RequiredArgsConstructor + final 字段

遵循这些规范,可以保证代码的可维护性、可扩展性和团队协作效率。

---

**相关文档**:
- `coding-standards.md` - 编码规范
- `database-guide.md` - 数据库设计
- `api-design.md` - API 设计规范
