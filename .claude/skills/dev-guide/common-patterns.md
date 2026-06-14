# 常用设计模式和最佳实践

本文档介绍 Snail AI 项目中使用的设计模式和最佳实践。

## 目录

- [Builder 模式](#builder-模式)
- [策略模式](#策略模式)
- [工厂模式](#工厂模式)
- [门面模式](#门面模式)
- [依赖注入](#依赖注入)
- [最佳实践](#最佳实践)

## Builder 模式

使用 Lombok `@Builder` 注解简化对象构建。

### 使用场景
- 对象有多个可选参数
- 需要链式调用
- 提高代码可读性

### 示例

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationMemoryDTO {
    private Long id;
    private Long agentId;
    private Long userId;
    private String content;
    private String memoryType;
    private BigDecimal relevanceScore;
    private LocalDateTime createDt;
}

// 使用 Builder
ConversationMemoryDTO memory = ConversationMemoryDTO.builder()
    .agentId(1L)
    .userId(100L)
    .content("重要信息")
    .memoryType(MemoryTypeEnum.FACT.getType())
    .relevanceScore(new BigDecimal("0.95"))
    .createDt(LocalDateTime.now())
    .build();
```

## 策略模式

用于实现多种可替换的算法或行为。

### 使用场景
- 多种 AI 模型切换
- 不同的文档解析器
- 多种向量存储实现

### 示例: AI 模型策略

```java
// 策略接口
public interface ChatModel {
    String call(String prompt);
}

// DeepSeek 实现
@Service
public class DeepSeekChatModel implements ChatModel {
    @Override
    public String call(String prompt) {
        // DeepSeek API 调用
        return response;
    }
}

// OpenAI 实现
@Service
public class OpenAIChatModel implements ChatModel {
    @Override
    public String call(String prompt) {
        // OpenAI API 调用
        return response;
    }
}

// 使用策略
@Service
@RequiredArgsConstructor
public class ChatService {
    private final Map<String, ChatModel> modelMap;
    
    public String chat(String modelType, String prompt) {
        ChatModel model = modelMap.get(modelType);
        return model.call(prompt);
    }
}
```

## 工厂模式

用于创建对象,隐藏创建逻辑。

### 使用场景
- 根据类型创建不同的对象
- Tool 工具创建
- 文档解析器创建

### 示例: Tool 工厂

```java
@Component
public class ToolFactory {
    
    private final ReadSkillTool readSkillTool;
    private final ShellTool shellTool;
    private final HttpTool httpTool;
    
    public List<Object> createTools(Long agentId) {
        List<Object> tools = new ArrayList<>();
        
        // 根据智能体配置创建不同的工具
        if (agentHasSkillEnabled(agentId)) {
            tools.add(readSkillTool);
            tools.add(shellTool);
            tools.add(httpTool);
        }
        
        return tools;
    }
}
```

## 门面模式

提供统一的对外接口,简化复杂的子系统调用。

### 使用场景
- Service 层封装多个子系统
- 统一对外 API

### 示例: Memory 门面

```java
@Service
@RequiredArgsConstructor
public class MemoryService {
    
    private final MemoryRetriever memoryRetriever;
    private final MemoryExtractionService memoryExtractionService;
    private final ConversationMemoryMapper memoryMapper;
    
    /**
     * 统一的记忆管理门面
     */
    public List<MemoryDTO> getRelevantMemories(Long agentId, String query) {
        // 1. 检索相关记忆
        List<ConversationMemoryPO> pos = memoryRetriever.retrieveRelevantMemories(
            agentId, userId, conversationId, query, null, modelId, limit, true);
        
        // 2. 更新访问统计
        pos.forEach(po -> memoryRetriever.updateAccessStats(po.getId()));
        
        // 3. 转换为 DTO
        return memoryRetriever.toDtoList(pos);
    }
}
```

## 依赖注入

使用 `@RequiredArgsConstructor` 实现构造函数注入。

### 为什么使用构造函数注入

✅ **优点**:
1. 不可变性 (final 字段)
2. 强制依赖 (编译时检查)
3. 易于测试
4. 循环依赖检测

### 示例

```java
@Service
@RequiredArgsConstructor  // Lombok 生成构造函数
public class UserService {
    
    // final 字段会被注入
    private final UserMapper userMapper;
    private final MemoryService memoryService;
    private final RedisHandler redisHandler;
    
    // 无需手写构造函数
}

// 等价于:
@Service
public class UserService {
    private final UserMapper userMapper;
    private final MemoryService memoryService;
    private final RedisHandler redisHandler;
    
    @Autowired
    public UserService(UserMapper userMapper, 
                       MemoryService memoryService,
                       RedisHandler redisHandler) {
        this.userMapper = userMapper;
        this.memoryService = memoryService;
        this.redisHandler = redisHandler;
    }
}
```

## 最佳实践

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

### 2. 参数封装

当方法参数超过 3 个时,使用对象封装。

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
    Long agentId = request.getAgentId();
    Long userId = request.getUserId();
    // ...
}
```

### 3. 提前返回(卫语句)

❌ **不推荐**:
```java
public void process(User user) {
    if (user != null) {
        if (user.isActive()) {
            if (user.hasPermission()) {
                // 业务逻辑
            }
        }
    }
}
```

✅ **推荐**:
```java
public void process(User user) {
    if (user == null) return;
    if (!user.isActive()) return;
    if (!user.hasPermission()) return;
    
    // 业务逻辑
}
```

### 4. Hutool 与 Optional 的使用边界

```java
// 不推荐
UserPO user = userMapper.selectById(id);
if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
    sendEmail(user.getEmail());
}

// 推荐：简单判断优先 Hutool
if (user != null && StrUtil.isNotBlank(user.getEmail())) {
    sendEmail(user.getEmail());
}

// 推荐：需要链式转换时使用 Optional
Optional.ofNullable(userMapper.selectById(id))
    .map(UserPO::getEmail)
    .filter(StrUtil::isNotBlank)
    .ifPresent(this::sendEmail);
```

### 5. Stream API 简化集合操作

```java
// 转换列表
List<UserVO> voList = userList.stream()
    .map(this::toVO)
    .collect(Collectors.toList());

// 过滤
List<UserPO> activeUsers = userList.stream()
    .filter(user -> user.getStatus() == StatusEnum.ACTIVE)
    .collect(Collectors.toList());

// 分组
Map<RoleEnum, List<UserPO>> usersByRole = userList.stream()
    .collect(Collectors.groupingBy(UserPO::getRole));
```

### 6. 异常处理

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    public UserVO getUser(Long id) {
        UserPO user = userMapper.selectById(id);
        
        // 业务异常
        if (user == null) {
            throw new SnailAiCommonException("用户不存在");
        }
        
        // 格式化参数
        if (!user.isActive()) {
            throw new SnailAiCommonException("用户 {} 已被禁用", user.getUsername());
        }
        
        return toVO(user);
    }
}
```

### 7. 事务管理

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    @Transactional(rollbackFor = Exception.class)
    public void createUserWithRole(UserPO user, RolePO role) {
        // 多个数据库操作在一个事务中
        userMapper.insert(user);
        role.setUserId(user.getId());
        roleMapper.insert(role);
    }
    
    @Transactional(readOnly = true)
    public List<UserPO> listUsers() {
        // 只读事务,优化性能
        return userMapper.selectList(null);
    }
}
```

### 8. 日志记录

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    public void processUser(Long userId) {
        log.info("开始处理用户: {}", userId);
        
        try {
            // 业务逻辑
            log.info("用户处理成功: {}", userId);
        } catch (Exception e) {
            log.error("用户处理失败: {}", userId, e);
            throw e;
        }
    }
}
```

### 9. 分层解耦

```java
// Controller 层：只负责接收和返回
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        return Result.ok(userService.getUser(id));
    }
}

// Service 层：业务逻辑
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    
    public UserVO getUser(Long id) {
        UserPO po = userMapper.selectById(id);
        return toVO(po);
    }
}

// Mapper 层：数据访问
public interface UserMapper extends BaseMapper<UserPO> {
}
```

### 10. VO/DTO/PO 转换

```java
@Service
public class UserService {
    
    // PO -> VO
    private UserVO toVO(UserPO po) {
        return UserVO.builder()
            .id(po.getId())
            .username(po.getUsername())
            .email(po.getEmail())
            .role(po.getRole())
            .createDt(po.getCreateDt())
            .build();
    }
    
    // VO -> PO
    private UserPO toPO(UserCreateVO vo) {
        return UserPO.builder()
            .username(vo.getUsername())
            .email(vo.getEmail())
            .role(RoleEnum.USER)
            .status(StatusEnum.ACTIVE)
            .createDt(LocalDateTime.now())
            .build();
    }
}
```

## 实际案例

### 案例: 记忆检索系统

```java
@Service
@RequiredArgsConstructor
public class MemoryRetriever {
    
    private final ConversationMemoryMapper memoryMapper;
    private final VectorStoreService vectorStoreService;
    
    /**
     * 检索相关记忆
     * 结合了策略模式(多种检索策略)和门面模式(统一接口)
     */
    public List<ConversationMemoryDTO> retrieveRelevantMemories(
            Long agentId, Long userId, String conversationId,
            String query, String memoryType, Long embeddingModelId,
            int limit, boolean useHybridSearch) {
        
        // 策略选择
        if (useHybridSearch) {
            return hybridSearch(agentId, userId, query, limit);
        } else {
            return semanticSearch(agentId, userId, query, limit);
        }
    }
    
    private List<ConversationMemoryDTO> hybridSearch(
            Long agentId, Long userId, String query, int limit) {
        
        // 1. 向量检索
        List<String> vectorIds = vectorStoreService.search(query, limit * 2);
        
        // 2. 数据库过滤
        LambdaQueryWrapper<ConversationMemoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMemoryPO::getAgentId, agentId)
               .eq(ConversationMemoryPO::getUserId, userId)
               .in(ConversationMemoryPO::getVectorId, vectorIds)
               .orderByDesc(ConversationMemoryPO::getRelevanceScore)
               .last(SqlLimit.of(limit));
        
        List<ConversationMemoryPO> pos = memoryMapper.selectList(wrapper);
        
        // 3. 转换为 DTO
        return toDtoList(pos);
    }
    
    public List<ConversationMemoryDTO> toDtoList(List<ConversationMemoryPO> pos) {
        return pos.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
}
```

## 总结

1. **Builder 模式**: 简化对象构建
2. **策略模式**: 多种算法可替换
3. **工厂模式**: 创建对象
4. **门面模式**: 统一接口
5. **依赖注入**: 使用构造函数注入
6. **最佳实践**: 禁止魔法值、参数封装、提前返回等

遵循这些模式和实践可以提高代码质量和可维护性。

**参考文档**:
- `/docs/CODE_STYLE.md` - 完整代码规范
- `coding-standards.md` - 编码规范
