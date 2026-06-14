package com.aizuda.snail.ai.admin.service.user;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.user.UserCreateRequestVO;
import com.aizuda.snail.ai.admin.vo.user.UserInfoVO;
import com.aizuda.snail.ai.admin.vo.user.UserQueryVO;
import com.aizuda.snail.ai.admin.vo.user.UserUpdateRequestVO;
import com.aizuda.snail.ai.common.enums.RoleEnum;
import com.aizuda.snail.ai.common.enums.StatusEnum;
import com.aizuda.snail.ai.common.exception.SnailAiCommonException;
import com.aizuda.snail.ai.common.model.PageDTO;
import com.aizuda.snail.ai.persistence.admin.mapper.UserMapper;
import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务
 * 
 * 标准的 Service 层示例:
 * - 使用 @Service 标记为服务层组件
 * - 使用 @Slf4j 自动注入日志
 * - 使用 @RequiredArgsConstructor 实现依赖注入
 * - 包含业务逻辑处理
 * - 处理数据转换（PO ↔ VO）
 * - 使用 @Transactional 管理事务
 *
 * @author opensnail
 * @date 2026-04-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    // 依赖注入
    private final UserMapper userMapper;
    
    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserInfoVO getUserInfo(Long userId) {
        log.info("获取用户信息: userId={}", userId);
        
        UserPO userPO = userMapper.selectById(userId);
        if (userPO == null) {
            throw new SnailAiCommonException("用户不存在");
        }
        
        return toUserInfoVO(userPO);
    }
    
    /**
     * 分页查询用户列表
     *
     * @param queryVO 查询参数
     * @return 分页结果
     */
    public PageResult<List<UserInfoVO>> getPageUserList(UserQueryVO queryVO) {
        log.info("分页查询用户: page={}, size={}, keyword={}", 
                queryVO.getPage(), queryVO.getSize(), queryVO.getKeyword());
        
        // 构建查询条件
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索（用户名或邮箱）
        if (StrUtil.isNotBlank(queryVO.getKeyword())) {
            wrapper.and(w -> w
                .like(UserPO::getUsername, queryVO.getKeyword())
                .or()
                .like(UserPO::getEmail, queryVO.getKeyword())
            );
        }
        
        // 排序
        wrapper.orderByDesc(UserPO::getCreateDt);
        
        // 执行分页查询
        PageDTO<UserPO> pageDTO = new PageDTO<>(queryVO.getPage(), queryVO.getSize());
        PageDTO<UserPO> result = userMapper.selectPage(pageDTO, wrapper);
        
        // 转换为 VO
        List<UserInfoVO> voList = result.getRecords().stream()
            .map(this::toUserInfoVO)
            .collect(Collectors.toList());
        
        return new PageResult<>(result.getTotal(), voList);
    }
    
    /**
     * 创建用户
     *
     * @param requestVO 创建请求
     * @return 创建的用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO createUser(UserCreateRequestVO requestVO) {
        log.info("创建用户: username={}", requestVO.getUsername());
        
        // 检查用户名是否存在
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<UserPO>()
            .eq(UserPO::getUsername, requestVO.getUsername());
        UserPO existingUser = userMapper.selectOne(wrapper);
        
        if (existingUser != null) {
            throw new SnailAiCommonException("用户名已存在");
        }
        
        // 构建 PO
        UserPO userPO = UserPO.builder()
            .username(requestVO.getUsername())
            .email(requestVO.getEmail())
            .password(encryptPassword(requestVO.getPassword()))
            .role(RoleEnum.USER)
            .status(StatusEnum.ACTIVE)
            .createDt(LocalDateTime.now())
            .updateDt(LocalDateTime.now())
            .build();
        
        // 插入数据库
        userMapper.insert(userPO);
        
        log.info("用户创建成功: userId={}, username={}", userPO.getId(), userPO.getUsername());
        
        return toUserInfoVO(userPO);
    }
    
    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param requestVO 更新请求
     * @return 更新后的用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO updateUser(Long userId, UserUpdateRequestVO requestVO) {
        log.info("更新用户: userId={}", userId);
        
        // 查询用户
        UserPO userPO = userMapper.selectById(userId);
        if (userPO == null) {
            throw new SnailAiCommonException("用户不存在");
        }
        
        // 更新字段
        if (StrUtil.isNotBlank(requestVO.getEmail())) {
            userPO.setEmail(requestVO.getEmail());
        }
        
        userPO.setUpdateDt(LocalDateTime.now());
        
        // 更新数据库
        userMapper.updateById(userPO);
        
        log.info("用户更新成功: userId={}", userId);
        
        return toUserInfoVO(userPO);
    }
    
    /**
     * 删除用户
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        log.info("删除用户: userId={}", userId);
        
        UserPO userPO = userMapper.selectById(userId);
        if (userPO == null) {
            throw new SnailAiCommonException("用户不存在");
        }
        
        // 软删除或硬删除
        userMapper.deleteById(userId);
        
        log.info("用户删除成功: userId={}", userId);
    }
    
    /**
     * 获取所有用户
     *
     * @return 用户列表
     */
    public List<UserInfoVO> listAllUsers() {
        log.info("获取所有用户");
        
        List<UserPO> userList = userMapper.selectList(null);
        
        return userList.stream()
            .map(this::toUserInfoVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 启用用户
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long userId) {
        log.info("启用用户: userId={}", userId);
        
        UserPO userPO = userMapper.selectById(userId);
        if (userPO == null) {
            throw new SnailAiCommonException("用户不存在");
        }
        
        userPO.setStatus(StatusEnum.ACTIVE);
        userPO.setUpdateDt(LocalDateTime.now());
        userMapper.updateById(userPO);
        
        log.info("用户启用成功: userId={}", userId);
    }
    
    /**
     * 禁用用户
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long userId) {
        log.info("禁用用户: userId={}", userId);
        
        UserPO userPO = userMapper.selectById(userId);
        if (userPO == null) {
            throw new SnailAiCommonException("用户不存在");
        }
        
        userPO.setStatus(StatusEnum.INACTIVE);
        userPO.setUpdateDt(LocalDateTime.now());
        userMapper.updateById(userPO);
        
        log.info("用户禁用成功: userId={}", userId);
    }
    
    /**
     * PO 转 VO
     *
     * @param userPO 用户PO
     * @return 用户VO
     */
    private UserInfoVO toUserInfoVO(UserPO userPO) {
        return UserInfoVO.builder()
            .id(userPO.getId())
            .username(userPO.getUsername())
            .email(userPO.getEmail())
            .role(userPO.getRole())
            .status(userPO.getStatus())
            .createDt(userPO.getCreateDt())
            .updateDt(userPO.getUpdateDt())
            .build();
    }
    
    /**
     * 密码加密（示例）
     *
     * @param password 明文密码
     * @return 加密后的密码
     */
    private String encryptPassword(String password) {
        // 实际项目中应使用 BCrypt 或其他加密算法
        return password; // 仅为示例
    }
}

/*
 * Service 层设计要点:
 * 
 * 1. 职责明确: Service 层包含业务逻辑,不包含 Web 层相关代码
 * 2. 事务管理: 使用 @Transactional 管理数据库事务
 * 3. 异常处理: 抛出业务异常（SnailAiCommonException）
 * 4. 日志记录: 记录关键业务操作
 * 5. 数据转换: 负责 PO 和 VO 之间的转换
 * 6. 参数验证: 验证业务规则（如用户名重复检查）
 * 7. 依赖注入: 使用构造函数注入
 * 
 * 文件位置参考:
 * /snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/service/user/UserService.java
 * /snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/service/memory/MemoryService.java
 */
