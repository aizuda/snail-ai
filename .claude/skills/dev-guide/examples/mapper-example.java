package com.aizuda.snail.ai.persistence.admin.mapper;

import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 用户 Mapper
 * 
 * 标准的 MyBatis-Plus Mapper 示例:
 * - 继承 BaseMapper<PO> 自动获得 CRUD 方法
 * - 无需编写 XML 配置文件
 * - 支持 Lambda 查询
 * 
 * @author opensnail
 * @date 2026-04-01
 */
public interface UserMapper extends BaseMapper<UserPO> {
    
    // 继承 BaseMapper 后，自动拥有以下方法：
    //
    // 插入操作:
    // - int insert(T entity)
    //
    // 删除操作:
    // - int deleteById(Serializable id)
    // - int deleteByMap(Map<String, Object> columnMap)
    // - int delete(Wrapper<T> wrapper)
    // - int deleteBatchIds(Collection<? extends Serializable> idList)
    //
    // 更新操作:
    // - int updateById(T entity)
    // - int update(T entity, Wrapper<T> updateWrapper)
    //
    // 查询操作:
    // - T selectById(Serializable id)
    // - List<T> selectBatchIds(Collection<? extends Serializable> idList)
    // - List<T> selectByMap(Map<String, Object> columnMap)
    // - T selectOne(Wrapper<T> queryWrapper)
    // - Long selectCount(Wrapper<T> queryWrapper)
    // - List<T> selectList(Wrapper<T> queryWrapper)
    // - List<Map<String, Object>> selectMaps(Wrapper<T> queryWrapper)
    // - IPage<T> selectPage(IPage<T> page, Wrapper<T> queryWrapper)
    //
    // 通常情况下，这些方法已经足够使用，无需自定义 SQL
    
    // ========== 可选：自定义 SQL 方法 ==========
    
    // 如果需要自定义 SQL，可以使用以下方式：
    
    // 方式 1: 使用注解 SQL
    // @Select("SELECT * FROM snail_ai_user WHERE username = #{username}")
    // UserPO findByUsername(@Param("username") String username);
    
    // 方式 2: 使用 XML 映射文件
    // 在 resources/mapper/admin/UserMapper.xml 中定义 SQL
    // List<UserPO> selectUserWithRole(@Param("roleId") Integer roleId);
    
}

/*
 * Mapper 使用示例:
 * 
 * ========== 基本 CRUD ==========
 * 
 * // 插入
 * UserPO user = UserPO.builder()
 *     .username("admin")
 *     .email("admin@example.com")
 *     .build();
 * userMapper.insert(user);  // 自动回填 id
 * 
 * // 根据 ID 查询
 * UserPO user = userMapper.selectById(1L);
 * 
 * // 更新
 * user.setEmail("newemail@example.com");
 * userMapper.updateById(user);
 * 
 * // 删除
 * userMapper.deleteById(1L);
 * 
 * 
 * ========== Lambda 查询 ==========
 * 
 * // 简单条件查询
 * LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
 * wrapper.eq(UserPO::getUsername, "admin");
 * UserPO user = userMapper.selectOne(wrapper);
 * 
 * // 多条件查询
 * LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
 * wrapper.eq(UserPO::getStatus, StatusEnum.ACTIVE)
 *        .like(UserPO::getUsername, "admin")
 *        .ge(UserPO::getCreateDt, startDate)
 *        .orderByDesc(UserPO::getCreateDt);
 * List<UserPO> users = userMapper.selectList(wrapper);
 * 
 * // 动态条件查询
 * LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
 * wrapper.eq(UserPO::getStatus, StatusEnum.ACTIVE);
 * 
 * if (StrUtil.isNotBlank(keyword)) {
 *     wrapper.like(UserPO::getUsername, keyword);
 * }
 * 
 * if (role != null) {
 *     wrapper.eq(UserPO::getRole, role);
 * }
 * 
 * List<UserPO> users = userMapper.selectList(wrapper);
 * 
 * 
 * ========== 分页查询 ==========
 * 
 * // 创建分页对象
 * PageDTO<UserPO> pageDTO = new PageDTO<>(page, size);
 * 
 * // 构建查询条件
 * LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
 * wrapper.eq(UserPO::getStatus, StatusEnum.ACTIVE)
 *        .orderByDesc(UserPO::getCreateDt);
 * 
 * // 执行分页查询
 * PageDTO<UserPO> result = userMapper.selectPage(pageDTO, wrapper);
 * 
 * // 获取结果
 * Long total = result.getTotal();        // 总记录数
 * List<UserPO> records = result.getRecords();  // 当前页数据
 * 
 * 
 * ========== 批量操作 ==========
 * 
 * // 批量查询
 * List<Long> ids = Arrays.asList(1L, 2L, 3L);
 * List<UserPO> users = userMapper.selectBatchIds(ids);
 * 
 * // 批量删除
 * userMapper.deleteBatchIds(ids);
 * 
 * 
 * ========== 统计查询 ==========
 * 
 * // 计数
 * LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
 * wrapper.eq(UserPO::getStatus, StatusEnum.ACTIVE);
 * Long count = userMapper.selectCount(wrapper);
 * 
 * 
 * ========== Lambda 更新 ==========
 * 
 * // 批量更新状态
 * LambdaUpdateWrapper<UserPO> updateWrapper = new LambdaUpdateWrapper<>();
 * updateWrapper.eq(UserPO::getStatus, StatusEnum.INACTIVE)
 *              .set(UserPO::getStatus, StatusEnum.ACTIVE)
 *              .set(UserPO::getUpdateDt, LocalDateTime.now());
 * 
 * userMapper.update(null, updateWrapper);
 * 
 * 
 * ========== 复杂查询 (OR 条件) ==========
 * 
 * // 用户名包含 "admin" 或 邮箱包含 "admin"
 * LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
 * wrapper.and(w -> w
 *     .like(UserPO::getUsername, "admin")
 *     .or()
 *     .like(UserPO::getEmail, "admin")
 * );
 * List<UserPO> users = userMapper.selectList(wrapper);
 * 
 * 
 * ========== 自定义 SQL (如需要) ==========
 * 
 * // 在 Mapper 接口中定义
 * @Select("SELECT COUNT(*) as total, role FROM snail_ai_user GROUP BY role")
 * List<Map<String, Object>> countByRole();
 * 
 * // 或使用 XML
 * <select id="selectUserWithRole" resultType="UserPO">
 *     SELECT u.*, r.role_name
 *     FROM snail_ai_user u
 *     LEFT JOIN snail_ai_role r ON u.role_id = r.id
 *     WHERE u.role_id = #{roleId}
 * </select>
 */

/*
 * Mapper 设计要点:
 * 
 * 1. 继承 BaseMapper: 自动获得常用 CRUD 方法
 * 2. 无需 XML: 大部分场景无需编写 XML 配置
 * 3. Lambda 查询: 类型安全,避免字段名拼写错误
 * 4. 动态条件: 使用 if 判断动态添加查询条件
 * 5. 分页查询: 使用 PageDTO 实现分页
 * 6. 批量操作: 使用 selectBatchIds, deleteBatchIds 等方法
 * 7. 自定义 SQL: 复杂查询时可使用 @Select 注解或 XML
 * 
 * 文件位置参考:
 * /snail-ai-persistence/src/main/java/com/aizuda/snail/ai/persistence/admin/mapper/UserMapper.java
 * /snail-ai-persistence/src/main/java/com/aizuda/snail/ai/persistence/memory/mapper/ConversationMemoryMapper.java
 */
