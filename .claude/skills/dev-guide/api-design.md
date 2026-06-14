# API 设计规范

本文档说明 Snail AI 项目的 RESTful API 设计规范。

## RESTful API 设计原则

### HTTP 方法

| 方法 | 用途 | 幂等性 | 示例 |
|------|------|--------|------|
| GET | 查询资源 | 是 | `/api/users/{id}` |
| POST | 创建资源 | 否 | `/api/users` |
| PUT | 完整更新资源 | 是 | `/api/users/{id}` |
| PATCH | 部分更新资源 | 否 | `/api/users/{id}/status` |
| DELETE | 删除资源 | 是 | `/api/users/{id}` |

### URL 设计

```
# 资源命名使用复数
GET    /api/users              # 获取用户列表
GET    /api/users/{id}         # 获取单个用户
POST   /api/users              # 创建用户
PUT    /api/users/{id}         # 更新用户
DELETE /api/users/{id}         # 删除用户

# 嵌套资源
GET    /api/agents/{agentId}/skills    # 获取智能体的技能列表
POST   /api/agents/{agentId}/skills    # 为智能体添加技能
```

## Controller 标准写法

### 基本结构

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
    
    @PutMapping("/{id}")
    @LoginRequired
    public Result<UserVO> updateUser(@PathVariable Long id, @RequestBody UserUpdateVO vo) {
        return Result.ok(userService.updateUser(id, vo));
    }
    
    @DeleteMapping("/{id}")
    @LoginRequired(role = RoleEnum.ADMIN)
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok(null);
    }
    
    @GetMapping("/page")
    @LoginRequired
    public PageResult<List<UserVO>> page(UserQueryVO queryVO) {
        return userService.page(queryVO);
    }
}
```

## 统一响应格式

### Result 类

**成功响应**:
```json
{
  "code": 1,
  "message": "Success",
  "data": {
    "id": 1,
    "username": "admin"
  }
}
```

**失败响应**:
```json
{
  "code": 0,
  "message": "用户不存在",
  "data": null
}
```

**Java 代码**:
```java
// 成功
Result<UserVO> result = Result.ok(userVO);

// 失败
Result<String> result = Result.fail("操作失败");
```

### PageResult 类

**分页响应**:
```json
{
  "total": 100,
  "list": [
    {"id": 1, "username": "user1"},
    {"id": 2, "username": "user2"}
  ]
}
```

**Java 代码**:
```java
PageResult<List<UserVO>> pageResult = new PageResult<>(total, list);
```

## 参数验证

### 请求参数验证

```java
public class UserCreateRequestVO {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20")
    private String password;
}
```

### Controller 使用验证

```java
@PostMapping
@LoginRequired
public Result<UserVO> createUser(@RequestBody @Validated UserCreateRequestVO vo) {
    return Result.ok(userService.createUser(vo));
}
```

## 认证和权限

### @LoginRequired 注解

```java
// 需要登录
@LoginRequired
public Result<UserVO> getProfile()

// 需要特定角色
@LoginRequired(role = RoleEnum.ADMIN)
public Result<Void> deleteUser(@PathVariable Long id)

@LoginRequired(role = RoleEnum.USER)
public Result<Void> updateProfile(@RequestBody ProfileVO vo)
```

## 异常处理

### 全局异常处理器

```java
@ControllerAdvice(basePackages = {"com.aizuda.snail.ai.admin"})
@Slf4j
public class RestExceptionHandler {
    
    @ExceptionHandler({BaseSnailAiException.class})
    public Result onBusinessException(BaseSnailAiException ex) {
        if (ex instanceof SnailAiAuthenticationException authenticationException) {
            return new Result<>(authenticationException.getErrorCode(), ex.getMessage());
        }
        return new Result<>(0, ex.getMessage());
    }
    
    @ExceptionHandler({Exception.class})
    public Result onException(Exception ex) {
        log.error("系统异常", ex);
        return Result.fail("系统异常,请联系管理员");
    }
}
```

## 实际案例

### 案例 1: 用户管理 API

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    @LoginRequired
    public Result<UserInfoVO> getUser(@PathVariable Long id) {
        return Result.ok(userService.getUserInfo(id));
    }
    
    /**
     * 获取用户列表（分页）
     */
    @GetMapping("/page")
    @LoginRequired(role = RoleEnum.ADMIN)
    public PageResult<List<UserInfoVO>> page(UserQueryVO queryVO) {
        return userService.getPageUserList(queryVO);
    }
    
    /**
     * 创建用户
     */
    @PostMapping
    @LoginRequired(role = RoleEnum.ADMIN)
    public Result<UserInfoVO> createUser(@RequestBody @Validated UserCreateVO vo) {
        UserInfoVO user = userService.createUser(vo);
        return Result.ok(user);
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @LoginRequired
    public Result<UserInfoVO> updateUser(
            @PathVariable Long id,
            @RequestBody @Validated UserUpdateVO vo) {
        return Result.ok(userService.updateUser(id, vo));
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @LoginRequired(role = RoleEnum.ADMIN)
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok(null);
    }
}
```

### 案例 2: 记忆管理 API

```java
@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
public class MemoryController {
    
    private final MemoryRetriever memoryRetriever;
    
    /**
     * 获取对话的记忆列表
     */
    @GetMapping("/conversation/{conversationId}")
    public Result<List<ConversationMemoryDTO>> getConversationMemories(
            @PathVariable String conversationId,
            MemoryConversationQueryVO queryVO) {
        
        UserPO user = UserSessionUtils.currentUserSession();
        List<ConversationMemoryPO> pos = memoryRetriever.retrieveConversationMemories(
            user.getId(), conversationId, queryVO.getLimit());
        
        return Result.ok(memoryRetriever.toDtoList(pos));
    }
    
    /**
     * 搜索记忆
     */
    @PostMapping("/search")
    public Result<List<ConversationMemoryDTO>> searchMemories(
            @RequestBody MemorySearchRequestVO request) {
        
        UserPO user = UserSessionUtils.currentUserSession();
        // 业务逻辑...
        return Result.ok(memories);
    }
    
    /**
     * 更新记忆
     */
    @PutMapping("/{memoryId}")
    public Result<ConversationMemoryDTO> updateMemory(
            @PathVariable Long memoryId,
            @RequestBody ConversationMemoryUpdateRequestVO dto) {
        
        // 业务逻辑...
        return Result.ok(updatedMemory);
    }
    
    /**
     * 删除记忆
     */
    @DeleteMapping("/{memoryId}")
    public Result<Boolean> deleteMemory(@PathVariable Long memoryId) {
        int rows = memoryMapper.deleteById(memoryId);
        return Result.ok(rows > 0);
    }
}
```

**文件位置**:
`/snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/controller/MemoryController.java`

## API 文档

### Swagger/OpenAPI (建议添加)

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户相关接口")
public class UserController {
    
    @Operation(summary = "获取用户信息", description = "根据ID获取用户详细信息")
    @Parameter(name = "id", description = "用户ID", required = true)
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        // ...
    }
}
```

## 最佳实践

1. **Controller 职责单一**: 只负责接收请求和返回响应
2. **业务逻辑在 Service**: 不要在 Controller 中写业务逻辑
3. **参数验证**: 使用 @Validated 进行参数验证
4. **统一响应格式**: 所有接口返回 Result 或 PageResult
5. **异常处理**: 使用全局异常处理器统一处理
6. **权限控制**: 使用 @LoginRequired 进行权限校验
7. **日志记录**: 关键操作记录日志

## 总结

遵循 RESTful API 设计规范可以:
- 提高 API 的可读性和可维护性
- 统一团队的 API 风格
- 便于前端对接和文档编写

**参考文档**:
- 项目实际代码: `/snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/controller/`
