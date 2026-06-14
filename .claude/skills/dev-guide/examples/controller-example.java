package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.service.user.UserService;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.vo.user.UserCreateRequestVO;
import com.aizuda.snail.ai.admin.vo.user.UserInfoVO;
import com.aizuda.snail.ai.admin.vo.user.UserQueryVO;
import com.aizuda.snail.ai.admin.vo.user.UserUpdateRequestVO;
import com.aizuda.snail.ai.common.enums.RoleEnum;
import com.aizuda.snail.ai.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理 Controller
 * 
 * 标准的 RESTful Controller 示例:
 * - 使用 @RestController 标记为 REST 控制器
 * - 使用 @RequestMapping 定义基础路径
 * - 使用 @RequiredArgsConstructor 实现构造函数注入
 * - 使用 @LoginRequired 进行权限控制
 * - 返回统一的 Result 或 PageResult 格式
 *
 * @author opensnail
 * @date 2026-04-01
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    // 依赖注入 - 使用 final 字段 + @RequiredArgsConstructor
    private final UserService userService;
    
    /**
     * 获取用户信息
     * 
     * GET /api/users/{id}
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    @LoginRequired  // 需要登录
    public Result<UserInfoVO> getUser(@PathVariable Long id) {
        UserInfoVO user = userService.getUserInfo(id);
        return Result.ok(user);
    }
    
    /**
     * 获取用户列表（分页）
     * 
     * GET /api/users/page?page=1&size=20&keyword=admin
     * 
     * @param queryVO 查询参数（page, size, keyword）
     * @return 分页结果
     */
    @GetMapping("/page")
    @LoginRequired(role = RoleEnum.ADMIN)  // 需要管理员权限
    public PageResult<List<UserInfoVO>> page(UserQueryVO queryVO) {
        return userService.getPageUserList(queryVO);
    }
    
    /**
     * 创建用户
     * 
     * POST /api/users
     * Body: {"username": "newuser", "email": "user@example.com", "password": "123456"}
     * 
     * @param requestVO 创建请求参数
     * @return 创建的用户信息
     */
    @PostMapping
    @LoginRequired(role = RoleEnum.ADMIN)  // 需要管理员权限
    public Result<UserInfoVO> createUser(@RequestBody @Validated UserCreateRequestVO requestVO) {
        UserInfoVO user = userService.createUser(requestVO);
        return Result.ok(user);
    }
    
    /**
     * 更新用户信息
     * 
     * PUT /api/users/{id}
     * Body: {"email": "newemail@example.com"}
     * 
     * @param id 用户ID
     * @param requestVO 更新请求参数
     * @return 更新后的用户信息
     */
    @PutMapping("/{id}")
    @LoginRequired  // 需要登录
    public Result<UserInfoVO> updateUser(
            @PathVariable Long id,
            @RequestBody @Validated UserUpdateRequestVO requestVO) {
        
        UserInfoVO user = userService.updateUser(id, requestVO);
        return Result.ok(user);
    }
    
    /**
     * 删除用户
     * 
     * DELETE /api/users/{id}
     * 
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @LoginRequired(role = RoleEnum.ADMIN)  // 需要管理员权限
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok(null);
    }
    
    /**
     * 获取所有用户（无分页）
     * 
     * GET /api/users/list
     * 
     * @return 用户列表
     */
    @GetMapping("/list")
    @LoginRequired(role = RoleEnum.ADMIN)
    public Result<List<UserInfoVO>> listAll() {
        List<UserInfoVO> users = userService.listAllUsers();
        return Result.ok(users);
    }
    
    /**
     * 启用用户
     * 
     * POST /api/users/{id}/enable
     * 
     * @param id 用户ID
     * @return 操作结果
     */
    @PostMapping("/{id}/enable")
    @LoginRequired(role = RoleEnum.ADMIN)
    public Result<Void> enableUser(@PathVariable Long id) {
        userService.enableUser(id);
        return Result.ok(null);
    }
    
    /**
     * 禁用用户
     * 
     * POST /api/users/{id}/disable
     * 
     * @param id 用户ID
     * @return 操作结果
     */
    @PostMapping("/{id}/disable")
    @LoginRequired(role = RoleEnum.ADMIN)
    public Result<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return Result.ok(null);
    }
}

/*
 * Controller 层设计要点:
 * 
 * 1. 职责单一: Controller 只负责接收请求和返回响应,不包含业务逻辑
 * 2. 参数验证: 使用 @Validated 进行参数验证
 * 3. 权限控制: 使用 @LoginRequired 进行权限校验
 * 4. 统一响应: 所有接口返回 Result<T> 或 PageResult<T>
 * 5. RESTful 风格: 遵循 RESTful API 设计规范
 * 6. 注释完整: 每个方法都有清晰的 Javadoc 注释
 * 7. 依赖注入: 使用构造函数注入,避免字段注入
 * 
 * 文件位置参考:
 * /snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/controller/UserController.java
 * /snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/controller/MemoryController.java
 */
