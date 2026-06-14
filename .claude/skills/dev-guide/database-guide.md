# 数据库和持久层指南

本文档说明 Snail AI 项目的数据库设计规范和 MyBatis-Plus 使用指南。

## 目录

- [数据库设计规范](#数据库设计规范)
- [PO 类设计](#po-类设计)
- [Mapper 接口](#mapper-接口)
- [查询操作](#查询操作)
- [事务管理](#事务管理)

## 数据库设计规范

### 表命名规范

- 统一前缀：`snail_ai_`
- 小写字母 + 下划线分隔
- 使用单数形式

**示例**:
```sql
snail_ai_user
snail_ai_conversation_memory
snail_ai_skill
snail_ai_rag_document
```

### 字段设计规范

| 字段类型 | 数据类型 | 说明 |
|---------|---------|------|
| 主键 | `BIGINT` | 自增,字段名 `id` |
| 时间戳 | `DATETIME` | 字段名后缀 `_dt` |
| 状态/类型 | `VARCHAR` 或 `ENUM` | 存储枚举值 |
| Boolean | `TINYINT(1)` | 0/1 |
| 金额 | `DECIMAL(10,2)` | 精确计算 |
| 文本 | `TEXT` / `LONGTEXT` | 大文本内容 |

### 必备字段

每个表建议包含：
```sql
CREATE TABLE snail_ai_example (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- 业务字段
    -- ...
    create_dt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 索引设计

- 主键自动创建聚簇索引
- 外键字段创建索引
- 常用查询条件创建索引
- 组合索引遵循最左前缀原则

**示例**:
```sql
-- 单列索引
CREATE INDEX idx_user_id ON snail_ai_conversation_memory(user_id);
CREATE INDEX idx_agent_id ON snail_ai_conversation_memory(agent_id);

-- 组合索引
CREATE INDEX idx_user_agent ON snail_ai_conversation_memory(user_id, agent_id);

-- 唯一索引
CREATE UNIQUE INDEX uk_username ON snail_ai_user(username);
```

## PO 类设计

### 标准 PO 类模板

```java
@TableName("snail_ai_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPO {
    
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 角色（枚举）
     */
    @EnumValue
    private RoleEnum role;
    
    /**
     * 状态
     */
    @EnumValue
    private StatusEnum status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createDt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateDt;
}
```

### 枚举字段映射

```java
@Getter
@AllArgsConstructor
public enum RoleEnum {
    ADMIN(1, "管理员"),
    USER(2, "用户");
    
    @EnumValue  // 标记存储到数据库的值
    private final Integer code;
    private final String desc;
}
```

**PO 中使用**:
```java
@EnumValue
private RoleEnum role;  // 数据库存储 code 值（1/2）
```

### 实际案例

**ConversationMemoryPO**:
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
    private String conversationId;
    private String sourceMessageId;
    
    private String memoryType;  // FACT|DECISION|PREFERENCE
    private String category;
    private String title;
    private String content;
    private String tags;  // JSON 存储
    
    private Long vectorStoreInstanceId;
    private String vectorId;
    
    private BigDecimal relevanceScore;
    private BigDecimal confidenceScore;
    
    @EnumValue
    private MemoryStatusEnum status;  // ACTIVE|ARCHIVED|SUPPRESSED
    
    private Integer accessCount;
    private LocalDateTime accessedAt;
    private LocalDateTime createDt;
    private LocalDateTime updateDt;
}
```

**文件位置**:
`/snail-ai-persistence/src/main/java/com/aizuda/snail/ai/persistence/memory/po/ConversationMemoryPO.java`

## Mapper 接口

### 标准 Mapper 接口

```java
public interface UserMapper extends BaseMapper<UserPO> {
    // 继承 BaseMapper 自动获得 CRUD 方法：
    // - insert(entity)
    // - deleteById(id)
    // - updateById(entity)
    // - selectById(id)
    // - selectOne(wrapper)
    // - selectList(wrapper)
    // - selectPage(page, wrapper)
    // 等等...
}
```

### 自定义 SQL (如需要)

```java
public interface UserMapper extends BaseMapper<UserPO> {
    
    /**
     * 自定义查询（使用注解）
     */
    @Select("SELECT * FROM snail_ai_user WHERE username = #{username}")
    UserPO findByUsername(@Param("username") String username);
    
    /**
     * 复杂查询（使用 XML）
     */
    List<UserPO> selectUserWithRole(@Param("roleId") Integer roleId);
}
```

## 查询操作

### 基本 CRUD

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    
    // 插入
    public void createUser(UserPO user) {
        userMapper.insert(user);
        // user.getId() 自动回填
    }
    
    // 查询单个
    public UserPO getUser(Long id) {
        return userMapper.selectById(id);
    }
    
    // 更新
    public void updateUser(UserPO user) {
        userMapper.updateById(user);
    }
    
    // 删除
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}
```

### Lambda 查询

**简单条件查询**:
```java
// 根据用户名查询
LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<UserPO>()
    .eq(UserPO::getUsername, username);
UserPO user = userMapper.selectOne(wrapper);

// 多条件查询
LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<UserPO>()
    .eq(UserPO::getStatus, StatusEnum.ACTIVE)
    .like(UserPO::getUsername, keyword)
    .orderByDesc(UserPO::getCreateDt);
List<UserPO> users = userMapper.selectList(wrapper);
```

**复杂条件查询**:
```java
LambdaQueryWrapper<ConversationMemoryPO> wrapper = new LambdaQueryWrapper<>();

// 必要条件
wrapper.eq(ConversationMemoryPO::getAgentId, agentId)
       .eq(ConversationMemoryPO::getUserId, userId);

// 可选条件
if (StrUtil.isNotBlank(conversationId)) {
    wrapper.eq(ConversationMemoryPO::getConversationId, conversationId);
}

if (memoryType != null) {
    wrapper.eq(ConversationMemoryPO::getMemoryType, memoryType);
}

// 时间范围
if (startDate != null) {
    wrapper.ge(ConversationMemoryPO::getCreateDt, startDate);
}

// 排序
wrapper.orderByDesc(ConversationMemoryPO::getRelevanceScore)
       .orderByDesc(ConversationMemoryPO::getCreateDt);

// 限制数量
wrapper.last(SqlLimit.of(limit));

List<ConversationMemoryPO> memories = memoryMapper.selectList(wrapper);
```

### 分页查询

```java
public PageResult<List<UserVO>> page(UserQueryVO queryVO) {
    // 构建查询条件
    LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
    
    if (StrUtil.isNotBlank(queryVO.getKeyword())) {
        wrapper.like(UserPO::getUsername, queryVO.getKeyword())
               .or()
               .like(UserPO::getEmail, queryVO.getKeyword());
    }
    
    // 创建分页对象
    PageDTO<UserPO> pageDTO = new PageDTO<>(queryVO.getPage(), queryVO.getSize());
    
    // 执行分页查询
    PageDTO<UserPO> resultPage = userMapper.selectPage(pageDTO, wrapper);
    
    // 转换为 VO
    List<UserVO> voList = resultPage.getRecords().stream()
        .map(this::convertToVO)
        .collect(Collectors.toList());
    
    return new PageResult<>(resultPage.getTotal(), voList);
}
```

### Lambda 更新

```java
// 批量更新
LambdaUpdateWrapper<UserPO> updateWrapper = new LambdaUpdateWrapper<>();
updateWrapper.eq(UserPO::getStatus, StatusEnum.INACTIVE)
             .set(UserPO::getStatus, StatusEnum.ACTIVE)
             .set(UserPO::getUpdateDt, LocalDateTime.now());

userMapper.update(null, updateWrapper);
```

### 统计查询

```java
// 计数
LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<UserPO>()
    .eq(UserPO::getStatus, StatusEnum.ACTIVE);
Long count = userMapper.selectCount(wrapper);

// 聚合查询（需要自定义 SQL）
@Select("SELECT COUNT(*) as total, role FROM snail_ai_user GROUP BY role")
List<Map<String, Object>> countByRole();
```

## 事务管理

### 使用 @Transactional

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    
    /**
     * 事务方法
     */
    @Transactional(rollbackFor = Exception.class)
    public void createUserWithRole(UserPO user, RolePO role) {
        // 插入用户
        userMapper.insert(user);
        
        // 插入角色关联
        role.setUserId(user.getId());
        userRoleMapper.insert(role);
        
        // 如果发生异常,两个操作都会回滚
    }
    
    /**
     * 只读事务（优化性能）
     */
    @Transactional(readOnly = true)
    public List<UserPO> listUsers() {
        return userMapper.selectList(null);
    }
}
```

### 事务传播行为

```java
// REQUIRED（默认）：如果当前存在事务,则加入该事务；否则创建新事务
@Transactional(propagation = Propagation.REQUIRED)

// REQUIRES_NEW：总是创建新事务,挂起当前事务
@Transactional(propagation = Propagation.REQUIRES_NEW)

// SUPPORTS：如果当前存在事务,则加入该事务；否则以非事务方式执行
@Transactional(propagation = Propagation.SUPPORTS)
```

## 实际案例

### 案例 1: 记忆检索

```java
public List<ConversationMemoryPO> retrieveRecentMemories(
        Long agentId, Long userId, int days, int limit) {
    
    LocalDateTime since = LocalDateTime.now().minusDays(days);
    
    LambdaQueryWrapper<ConversationMemoryPO> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ConversationMemoryPO::getAgentId, agentId)
           .eq(ConversationMemoryPO::getUserId, userId)
           .eq(ConversationMemoryPO::getStatus, MemoryStatusEnum.ACTIVE)
           .ge(ConversationMemoryPO::getCreateDt, since)
           .orderByDesc(ConversationMemoryPO::getCreateDt)
           .last(SqlLimit.of(limit));
    
    return memoryMapper.selectList(wrapper);
}
```

**文件位置**:
`/snail-ai-memory/src/main/java/com/aizuda/snail/ai/memory/service/MemoryRetriever.java`

### 案例 2: 用户分页查询

```java
public PageResult<List<UserInfoVO>> getPageUserList(UserQueryVO queryVO) {
    LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
    
    // 关键词搜索
    if (StrUtil.isNotBlank(queryVO.getKeyword())) {
        wrapper.and(w -> w
            .like(UserPO::getUsername, queryVO.getKeyword())
            .or()
            .like(UserPO::getEmail, queryVO.getKeyword())
        );
    }
    
    // 状态过滤
    if (queryVO.getStatus() != null) {
        wrapper.eq(UserPO::getStatus, queryVO.getStatus());
    }
    
    // 排序
    wrapper.orderByDesc(UserPO::getCreateDt);
    
    // 分页查询
    PageDTO<UserPO> pageDTO = new PageDTO<>(queryVO.getPage(), queryVO.getSize());
    PageDTO<UserPO> result = userMapper.selectPage(pageDTO, wrapper);
    
    // 转换为 VO
    List<UserInfoVO> voList = result.getRecords().stream()
        .map(this::toUserInfoVO)
        .collect(Collectors.toList());
    
    return new PageResult<>(result.getTotal(), voList);
}
```

## 性能优化建议

1. **使用索引**: 为常用查询字段创建索引
2. **避免 SELECT ***: 只查询需要的字段
3. **合理分页**: 不要一次查询太多数据
4. **批量操作**: 使用 `insertBatch()` 替代循环 `insert()`
5. **缓存热点数据**: 使用 Redis 缓存常用数据
6. **读写分离**: 读操作使用从库

## 总结

- 使用 MyBatis-Plus 简化 CRUD 操作
- Lambda 查询提供类型安全
- 合理使用事务保证数据一致性
- 遵循命名规范和索引设计规范

**参考文档**:
- MyBatis-Plus 官方文档：https://baomidou.com/
- 项目实际代码：`/snail-ai-persistence/`
