# 命名规范速查表

本文档提供快速查询的命名规范表格。

## 类命名速查表

| 类型 | 命名规则 | 示例 | 用途 |
|-----|---------|------|------|
| **VO** | `{Name}VO` | `UserInfoVO`, `MemoryStatsVO` | API 响应、页面展示 |
| **DTO** | `{Name}DTO` | `ConversationMemoryDTO` | 跨模块传输 |
| **PO** | `{Name}PO` | `UserPO`, `ConversationMemoryPO` | 数据库表映射 |
| **Mapper** | `{Name}Mapper` | `UserMapper`, `MemoryMapper` | MyBatis Plus 数据访问 |
| **Service** ⚠️ | `{Name}Service` | `UserService`, `MemoryService` | **仅 admin 模块**使用 |
| **Handler** ⚠️ | `{Name}Handler` | `MemoryRetriever`, `RedisHandler` | **其他模块**使用 |
| **Controller** | `{Name}Controller` | `UserController` | Web 控制器 |
| **Enum** | `{Name}Enum` | `RoleEnum`, `StatusEnum` | 枚举类型 |
| **Exception** | `{Name}Exception` | `SnailAiCommonException` | 异常类 |
| **Constants** | `{Name}Constants` | `SystemConstants` | 常量类 |
| **Config** | `{Name}Config` | `RedisConfig`, `MemoryConfig` | 配置类 |
| **Tool** | `{Name}Tool` | `ReadSkillTool`, `ShellTool` | LangChain4j 工具 |

## ⚠️ Service vs Handler 重要规则

**根据模块选择命名**:

| 模块类型 | 使用后缀 | 示例 | 说明 |
|---------|---------|------|------|
| **admin** | `Service` | `UserService`, `MemoryService`, `SkillService` | 管理后台业务逻辑 |
| **memory** | `Handler` | `MemoryRetriever`, `MemoryExtractionService` | 记忆处理逻辑 |
| **features** | `Handler` | `RagService`, `SkillService` (features层) | 功能处理逻辑 |
| **common** | `Handler` | `RedisHandler`, `RestExceptionHandler` | 通用处理器 |
| **model** | `Handler` | `ModelHandler` | 模型处理器 |

**注意**: 虽然其他模块也可能使用 `@Service` 注解,但类名应使用 `Handler` 后缀或特定名称(如 `MemoryRetriever`)

## 方法命名速查表

| 操作类型 | 前缀 | 示例 | 说明 |
|---------|-----|------|------|
| **查询** | `get`, `query`, `fetch`, `retrieve` | `getUserInfo()`, `queryMemories()` | 获取数据 |
| **列表** | `list`, `get{Name}List` | `listUsers()`, `getMemoryList()` | 获取列表 |
| **分页** | `page` | `page(QueryVO)` | 分页查询 |
| **创建** | `create`, `add`, `insert` | `createUser()`, `addMemory()` | 新增数据 |
| **更新** | `update`, `modify`, `set` | `updateUser()`, `setStatus()` | 修改数据 |
| **删除** | `delete`, `remove`, `del` | `deleteUser()`, `removeMemory()` | 删除数据 |
| **判断** | `is`, `has`, `check`, `verify` | `isActive()`, `hasPermission()` | 布尔判断 |
| **转换** | `to`, `convert` | `toVO()`, `convertToDTO()` | 类型转换 |
| **构建** | `build` | `buildFileTree()`, `buildSkillsList()` | 构建对象 |
| **处理** | `process`, `handle` | `processRequest()`, `handleError()` | 业务处理 |

## 字段命名速查表

| 字段类型 | 命名规则 | 示例 | 说明 |
|---------|---------|------|------|
| **Boolean** | `is{Name}` 或现在分词 | `isActive`, `autoExtract`, `memoryEnabled` | 布尔值 |
| **时间** | 后缀 `Dt` 或 `At` | `createDt`, `updateDt`, `accessedAt` | 时间戳 |
| **ID** | 后缀 `Id` | `id`, `userId`, `agentId`, `conversationId` | 主键/外键 |
| **状态** | `status` | `status` (枚举) | 状态字段 |
| **类型** | `type` 或 `{name}Type` | `memoryType`, `fileType` | 类型字段 |
| **计数** | 后缀 `Count` | `accessCount`, `messageCount` | 计数字段 |
| **评分** | 后缀 `Score` | `relevanceScore`, `confidenceScore` | 评分字段 |
| **名称** | `name` 或 `{name}Name` | `username`, `fileName` | 名称字段 |
| **内容** | `content` | `content`, `skillContent` | 文本内容 |
| **路径** | 后缀 `Path` | `filePath`, `storagePath` | 文件路径 |
| **URL** | `url` 或 `{name}Url` | `url`, `callbackUrl` | 链接地址 |
| **列表** | 后缀 `List` 或复数 | `skillIds`, `tags`, `memories` | 列表/数组 |

## 包命名速查表

| 包名 | 用途 | 示例 |
|-----|------|------|
| `config` | 配置类 | `com.aizuda.snail.ai.admin.config` |
| `constant` | 常量定义 | `com.aizuda.snail.ai.common.constant` |
| `controller` | Web 控制器 | `com.aizuda.snail.ai.admin.controller` |
| `service` | 业务服务 | `com.aizuda.snail.ai.admin.service` |
| `dto` | 数据传输对象 | `com.aizuda.snail.ai.memory.dto` |
| `vo` | 视图对象 | `com.aizuda.snail.ai.admin.vo` |
| `po` | 持久化对象 | `com.aizuda.snail.ai.persistence.memory.po` |
| `mapper` | 数据访问层 | `com.aizuda.snail.ai.persistence.memory.mapper` |
| `enums` | 枚举类型 | `com.aizuda.snail.ai.common.enums` |
| `exception` | 异常定义 | `com.aizuda.snail.ai.common.exception` |
| `handler` | 处理器 | `com.aizuda.snail.ai.admin.handler` |
| `interceptor` | 拦截器 | `com.aizuda.snail.ai.admin.interceptor` |
| `security` | 安全相关 | `com.aizuda.snail.ai.admin.security` |
| `util` | 工具类 | `com.aizuda.snail.ai.common.util` |
| `tool` | LangChain4j 工具 | `com.aizuda.snail.ai.features.skill.tool` |

## 常量命名规范

```java
// 全大写,单词间用下划线分隔
public class SystemConstants {
    public static final String DEFAULT_ROLE = "USER";
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int DEFAULT_PAGE_SIZE = 20;
}
```

## 枚举命名规范

```java
@Getter
@AllArgsConstructor
public enum RoleEnum {
    ADMIN(1, "管理员"),
    USER(2, "用户");
    
    @EnumValue  // 存储到数据库的值
    private final Integer code;
    private final String desc;
}
```

## 数据库命名规范

| 类型 | 命名规则 | 示例 |
|-----|---------|------|
| **表名** | `snail_ai_{name}` | `snail_ai_user`, `snail_ai_conversation_memory` |
| **字段** | 小写+下划线 | `user_id`, `create_dt`, `memory_type` |
| **主键** | `id` | `id BIGINT AUTO_INCREMENT` |
| **外键** | `{table}_id` | `user_id`, `agent_id` |
| **时间** | 后缀 `_dt` 或 `_at` | `create_dt`, `update_dt`, `accessed_at` |
| **索引** | `idx_{field}` | `idx_user_id`, `idx_create_dt` |
| **唯一索引** | `uk_{field}` | `uk_username` |

## 快速示例

### 完整类示例

```java
// VO 类
@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String email;
    private RoleEnum role;
    private LocalDateTime createDt;
}

// PO 类
@TableName("snail_ai_user")
@Data
@Builder
public class UserPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @EnumValue
    private RoleEnum role;
    private LocalDateTime createDt;
}

// Service 类
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    
    public UserInfoVO getUserInfo(Long id) {
        // ...
    }
    
    public void createUser(UserCreateVO vo) {
        // ...
    }
}

// Controller 类
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    @GetMapping("/{id}")
    @LoginRequired
    public Result<UserInfoVO> getUser(@PathVariable Long id) {
        return Result.ok(userService.getUserInfo(id));
    }
}
```

## 注意事项

1. **一致性**: 同一项目中保持命名风格一致
2. **可读性**: 名称要见名知意,避免使用缩写
3. **简洁性**: 避免过长的名称
4. **驼峰命名**: Java 类和方法使用驼峰命名(camelCase/PascalCase)
5. **下划线命名**: 数据库表和字段使用下划线(snake_case)

## 总结

本速查表提供常用的命名规范,开发时可快速查询参考。遵循统一的命名规范可以提高代码可读性和可维护性。

**相关文档**:
- `coding-standards.md` - 详细编码规范
- `/docs/CODE_STYLE.md` - 完整代码风格指南
