# 编码规范详解

本文档详细说明 Snail AI 项目的编码规范,包括命名、注解、注释和代码风格。

## 目录

- [类命名规范](#类命名规范)
- [方法命名规范](#方法命名规范)
- [字段命名规范](#字段命名规范)
- [注解使用规范](#注解使用规范)
- [注释规范](#注释规范)
- [日志规范](#日志规范)
- [代码风格](#代码风格)

## 类命名规范

### VO 类 (View Object)

**命名规则**: `{Name}VO`

**用途**: API 响应、页面展示

**示例**:
```java
// 响应 VO
public class UserInfoVO {
    private Long id;
    private String username;
    private String email;
}

// 查询 VO (继承 BaseQueryVO)
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryVO extends BaseQueryVO {
    private String keyword;
    // 继承: page, size
}

// 请求 VO
public class UserCreateRequestVO {
    @NotBlank
    private String username;
    @Email
    private String email;
}
```

**实际代码位置**:
- `/snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/vo/memory/MemoryStatsVO.java`
- `/snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/vo/UserInfoVO.java`

### DTO 类 (Data Transfer Object)

**命名规则**: `{Name}DTO`

**用途**: 跨模块数据传输、请求参数封装

**示例**:
```java
@Data
@Builder
public class ConversationMemoryDTO {
    private Long id;
    private Long agentId;
    private String content;
    private String memoryType;
    private BigDecimal relevanceScore;
    private LocalDateTime createDt;
}
```

**实际代码位置**:
- `/snail-ai-memory/src/main/java/com/aizuda/snail/ai/memory/dto/ConversationMemoryDTO.java`

### PO 类 (Persistent Object)

**命名规则**: `{Name}PO`

**用途**: 与数据库表一一对应

**示例**:
```java
@TableName("snail_ai_conversation_memory")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationMemoryPO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long agentId;
    private Long userId;
    private String content;
    
    @EnumValue
    private MemoryStatusEnum status;
    
    private LocalDateTime createDt;
    private LocalDateTime updateDt;
}
```

**实际代码位置**:
- `/snail-ai-persistence/src/main/java/com/aizuda/snail/ai/persistence/memory/po/ConversationMemoryPO.java`

### Service/Handler 命名规范 ⚠️ 重要

**规则**: 根据模块不同使用不同后缀

| 模块 | 命名规则 | 示例 | 说明 |
|-----|---------|------|------|
| **admin 模块** | `{Name}Service` | `UserService`, `MemoryService` | 管理后台的业务逻辑层 |
| **其他模块** | `{Name}Handler` | `MemoryRetriever`, `RedisHandler` | 功能处理器 |

**示例**:

```java
// ✅ admin 模块 - 使用 Service
// 文件: snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/service/user/UserService.java
@Service
public class UserService {
    // 管理后台的用户业务逻辑
}

// ✅ features/memory 模块 - 使用 Handler
// 文件: snail-ai-memory/src/main/java/com/aizuda/snail/ai/memory/service/MemoryRetriever.java
@Service
public class MemoryRetriever {
    // 记忆检索处理
}

// ✅ common 模块 - 使用 Handler
// 文件: snail-ai-common/src/main/java/com/aizuda/snail/ai/common/handler/RedisHandler.java
@Component
public class RedisHandler {
    // Redis 操作处理
}
```

**实际代码位置**:
- Admin Service: `/snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/service/`
- Memory Handler: `/snail-ai-memory/src/main/java/com/aizuda/snail/ai/memory/service/MemoryRetriever.java`
- Features Handler: `/snail-ai-features/src/main/java/com/aizuda/snail/ai/features/`

### 其他类型

| 类型 | 命名规则 | 示例 |
|-----|---------|------|
| Mapper | `{Name}Mapper` | `UserMapper`, `ConversationMemoryMapper` |
| Controller | `{Name}Controller` | `UserController`, `MemoryController` |
| Enum | `{Name}Enum` | `RoleEnum`, `MemoryStatusEnum` |
| Exception | `{Name}Exception` | `SnailAiCommonException` |
| Constants | `{Name}Constants` | `CommonConstants`, `SystemConstants` |
| Config | `{Name}Config` | `RedisConfig`, `MemoryConfig` |
| Tool | `{Name}Tool` | `ReadSkillTool`, `ShellTool` |

## 方法命名规范

### 查询方法

**前缀**: `get`, `query`, `fetch`, `retrieve`, `find`

```java
// 获取单个对象
public UserInfoVO getUserInfo(Long id)
public UserPO getUserById(Long id)

// 获取列表
public List<MemoryDTO> getMemories(Long agentId)
public List<ConversationResponseVO> getConversationList(String userId)

// 分页查询
public PageResult<List<UserVO>> page(UserQueryVO queryVO)
public PageDTO<UserPO> getUserPage(int page, int size)

// 检索记忆
public List<ConversationMemoryDTO> retrieveRelevantMemories(
    Long agentId, Long userId, String query, int limit)
```

### 创建方法

**前缀**: `create`, `add`, `insert`

```java
public UserVO createUser(UserCreateRequestVO request)
public void addMemory(AddMemoryRequest request)
public Long insert(UserPO userPO)
```

### 更新方法

**前缀**: `update`, `modify`, `set`

```java
public void updateUser(Long id, UserUpdateVO vo)
public boolean updateConversationTitle(UpdateConversationTitleRequestVO requestVO)
public void setStatus(Long id, StatusEnum status)
```

### 删除方法

**前缀**: `delete`, `remove`, `del`

```java
public void deleteUser(Long id)
public boolean delConversationByConversationId(String conversationId)
public void remove(Long id)
```

### 判断方法

**前缀**: `is`, `has`, `check`, `verify`, `contains`

```java
public static boolean isAdmin(Integer roleId)
public boolean hasPermission(Long userId, String permission)
public boolean checkUserExists(String username)
```

### 业务操作方法

使用**具体的动词**:

```java
public LoginResponseVO login(LoginRequestVO requestVO)
public void authorize(AuthorizeRequestVO requestVO)
public boolean archiveMemory(Long memoryId)
public boolean suppressMemory(Long memoryId)
public void register(LoginRequestVO requestVO)
```

## 字段命名规范

### Boolean 字段

**规则**: 使用 `is` 前缀或现在分词

```java
// is 前缀
private Boolean isActive;
private Boolean isDeleted;

// 现在分词
private Boolean autoExtract;  // 自动提取
private Boolean memoryEnabled;  // 记忆启用
private Boolean relevanceUsed;  // 相关性使用
```

### 时间字段

**规则**: 后缀 `Dt` (DateTime)

```java
private LocalDateTime createDt;
private LocalDateTime updateDt;
private LocalDateTime accessedAt;  // 特殊情况可以用 At
private LocalDateTime expiresAt;
```

### ID 字段

**规则**: 后缀 `Id`

```java
private Long id;           // 主键
private Long userId;
private Long agentId;
private Long memoryId;
private Long vectorStoreInstanceId;
private String conversationId;  // 字符串 ID
```

### 状态/类型字段

**规则**: 使用枚举或对应的英文名

```java
// 枚举
@EnumValue
private MemoryStatusEnum status;  // ACTIVE|ARCHIVED|SUPPRESSED

// 字符串字段必须对应 enum 或常量，不裸写业务值
private String memoryType;
private String actorRole;
```

### 计数字段

**规则**: 后缀 `Count`

```java
private Integer accessCount;
private Integer totals;  // 特殊情况
private Long messageCount;
```

### 评分字段

**规则**: 后缀 `Score`

```java
private BigDecimal relevanceScore;
private BigDecimal confidenceScore;
private Integer feedbackScore;
```

## 注解使用规范

### Spring 注解

```java
// Web 层
@RestController                    // 标记为 REST 控制器
@RequestMapping("/api/users")      // 基础路径
@GetMapping("/{id}")               // GET 请求
@PostMapping                       // POST 请求
@PutMapping("/{id}")               // PUT 请求
@DeleteMapping("/{id}")            // DELETE 请求
@PathVariable Long id              // 路径变量
@RequestParam String keyword       // 请求参数
@RequestBody UserVO vo             // 请求体
@Validated                         // 参数验证

// 服务层
@Service                           // 标记为服务
@Transactional                     // 事务管理

// 配置层
@Configuration                     // 配置类
@ConfigurationProperties(prefix = "snail-ai")  // 配置绑定
@Bean                              // Bean 定义
@EnableAsync                       // 启用异步
```

### Lombok 注解

```java
// 数据类
@Data                              // getter/setter/toString/equals/hashCode
@Getter                            // 仅 getter
@Setter                            // 仅 setter
@Builder                           // Builder 模式
@AllArgsConstructor                // 全参构造
@NoArgsConstructor                 // 无参构造
@RequiredArgsConstructor           // 必需字段构造（final 字段）

// 继承
@EqualsAndHashCode(callSuper = true)  // equals/hashCode 包含父类

// 日志
@Slf4j                             // 自动注入 logger
```

**完整示例**:
```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserVO {
    private Long id;
    private String username;
    private LocalDateTime createDt;
}
```

### MyBatis-Plus 注解

```java
@TableName("snail_ai_user")        // 表名映射
@TableId(type = IdType.AUTO)       // 自增主键
@TableField("user_name")           // 字段映射
@EnumValue                          // 枚举值映射到数据库
```

**完整示例**:
```java
@TableName("snail_ai_user")
@Data
public class UserPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_name")
    private String username;
    
    @EnumValue
    private RoleEnum role;
}
```

### 验证注解

```java
@NotNull(message = "id 不能为空")
@NotBlank(message = "用户名不能为空")
@NotEmpty(message = "列表不能为空")
@Email(message = "邮箱格式不正确")
@Size(min = 6, max = 20, message = "密码长度 6-20")
@Min(value = 1, message = "最小值为 1")
@Max(value = 100, message = "最大值为 100")
```

**示例**:
```java
public class UserCreateRequestVO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Size(min = 6, max = 20, message = "密码长度 6-20")
    private String password;
}
```

### 自定义注解

```java
// 登录认证
@LoginRequired                      // 需要登录
@LoginRequired(role = RoleEnum.ADMIN)  // 需要管理员权限
@LoginRequired(role = RoleEnum.USER)   // 需要用户权限
```

**使用示例**:
```java
@GetMapping("/admin/users")
@LoginRequired(role = RoleEnum.ADMIN)
public PageResult<List<UserVO>> listUsers(UserQueryVO queryVO) {
    return userService.page(queryVO);
}
```

## 注释规范

### Javadoc 注释

**类级别**:
```java
/**
 * 用户管理服务
 *
 * @author opensnail
 * @date 2025-07-19
 */
@Service
public class UserService {
}
```

**方法级别**:
```java
/**
 * 获取用户信息
 *
 * @param userId 用户ID
 * @return 用户信息
 */
public UserVO getUserInfo(Long userId) {
    // ...
}
```

### 行内注释

```java
// 检查用户是否存在
UserPO user = userMapper.selectById(userId);
if (user == null) {
    throw new SnailAiCommonException("用户不存在");
}

// 更新用户状态
user.setStatus(StatusEnum.ACTIVE);
userMapper.updateById(user);
```

### TODO 注释

```java
// TODO: 实现用户权限校验
// TODO: 优化查询性能
// FIXME: 修复并发问题
```

## 日志规范

### 使用 @Slf4j

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    
    public void register(LoginRequestVO requestVO) {
        // 业务逻辑
        log.info("新用户注册成功: {}", requestVO.getUsername());
    }
    
    public LoginResponseVO login(LoginRequestVO requestVO) {
        try {
            // 登录逻辑
            log.info("用户登录成功: {}", requestVO.getUsername());
            return loginResponse;
        } catch (Exception e) {
            log.error("用户登录失败: {}", requestVO.getUsername(), e);
            throw new SnailAiCommonException("登录失败");
        }
    }
}
```

### 日志级别使用

| 级别 | 用途 | 示例 |
|-----|------|------|
| **info** | 重要业务操作 | `log.info("用户注册成功: {}", username)` |
| **error** | 异常情况 | `log.error("处理失败", e)` |
| **debug** | 调试信息 | `log.debug("查询参数: {}", params)` |
| **warn** | 警告信息 | `log.warn("配额即将用尽")` |

### 日志格式

```java
// 使用 {} 占位符
log.info("用户 {} 登录成功", username);
log.error("处理订单 {} 失败: {}", orderId, e.getMessage(), e);

// 避免字符串拼接
// ❌ 不推荐
log.info("用户 " + username + " 登录成功");

// ✅ 推荐
log.info("用户 {} 登录成功", username);
```

## 代码风格

### 缩进和格式

- 使用 **4 个空格** 缩进（不使用 Tab）
- 每行最多 **120** 个字符
- 大括号不换行（K&R 风格）

```java
// ✅ 推荐
public void process() {
    if (condition) {
        doSomething();
    }
}

// ❌ 不推荐
public void process()
{
    if (condition)
    {
        doSomething();
    }
}
```

### 空行使用

```java
public class UserService {
    
    private final UserMapper userMapper;  // 字段声明后空一行
    
    public UserVO getUser(Long id) {
        // 方法内部逻辑分组之间空一行
        UserPO po = userMapper.selectById(id);
        
        if (po == null) {
            throw new SnailAiCommonException("用户不存在");
        }
        
        return convertToVO(po);
    }
    
    public void updateUser(Long id, UserUpdateVO vo) {  // 方法之间空一行
        // ...
    }
}
```

### Import 顺序

1. Java 标准库
2. 第三方库
3. Spring 框架
4. 项目内部包

```java
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import com.aizuda.snail.ai.admin.vo.UserVO;
import com.aizuda.snail.ai.common.model.Result;
```

### 常量定义

```java
public class UserConstants {
    // 全大写,单词间用下划线分隔
    public static final String DEFAULT_ROLE = "USER";
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final BigDecimal DEFAULT_QUOTA = new BigDecimal("100.00");
}
```

## 总结

遵循这些编码规范可以：
1. 提高代码可读性和可维护性
2. 减少代码审查成本
3. 保持团队代码风格一致
4. 降低新人上手难度

**实际参考**:
- 项目现有代码：`/snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/`
- 完整规范文档：`/docs/CODE_STYLE.md`
