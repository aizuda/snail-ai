package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.common.model.Result;
import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.vo.PageResult;
import com.aizuda.snail.ai.admin.enums.RoleEnum;
import com.aizuda.snail.ai.admin.vo.AuthorizeRequestVO;
import com.aizuda.snail.ai.admin.vo.ChangePasswordRequestVO;
import com.aizuda.snail.ai.admin.vo.LoginRequestVO;
import com.aizuda.snail.ai.admin.vo.LoginResponseVO;
import com.aizuda.snail.ai.admin.vo.ResetPasswordRequestVO;
import com.aizuda.snail.ai.admin.vo.UserCreateRequestVO;
import com.aizuda.snail.ai.admin.vo.UserInfoVO;
import com.aizuda.snail.ai.admin.vo.UserQueryVO;
import com.aizuda.snail.ai.admin.vo.UserUpdateRequestVO;
import com.aizuda.snail.ai.admin.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 *
 * @author opensnail
 * @date 2025-07-12
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * 用户登录（账号密码）
     */
    @PostMapping("/login")
    public LoginResponseVO login(@RequestBody @Valid LoginRequestVO requestVO) {
        return userService.login(requestVO);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody @Valid LoginRequestVO requestVO) {
        userService.register(requestVO);
        return Result.ok("注册成功", "注册成功");
    }

    /**
     * 获取用户信息
     */
    @GetMapping
    @LoginRequired(role = RoleEnum.USER)
    public UserInfoVO getUserInfo() {
       return userService.getUserInfo();
    }

    /**
     * 创建用户（管理员）
     */
    @PostMapping
    @LoginRequired(role = RoleEnum.ADMIN)
    public Result<String> createUser(@RequestBody @Valid UserCreateRequestVO requestVO) {
        userService.createUser(requestVO);
        return Result.ok("创建成功", "创建成功");
    }

    /**
     * 获取用户列表（管理员）
     */
    @GetMapping("/page/list")
    @LoginRequired(role = RoleEnum.ADMIN)
    public PageResult<List<UserInfoVO>> getPageUserList(UserQueryVO queryVO) {
       return userService.getPageUserList(queryVO);
    }

    /**
     * 授权用户（管理员）
     */
    @PostMapping("/authorize/code")
    @LoginRequired(role = RoleEnum.ADMIN)
    public Result<String> authorizeUser(@RequestBody @Valid AuthorizeRequestVO requestVO) {
        userService.authorizeUser(requestVO);
        return Result.ok("授权成功", "授权成功");
    }

    /**
     * 更新用户信息（管理员）- 统一更新接口
     */
    @PutMapping("/{id}")
    @LoginRequired(role = RoleEnum.ADMIN)
    public Result<String> updateUser(@PathVariable("id") Long id, @RequestBody @Valid UserUpdateRequestVO requestVO) {
        userService.updateUser(id, requestVO);
        return Result.ok("更新成功", "更新成功");
    }

    /**
     * 更新用户角色（管理员）
     */
    @PutMapping("/{id}/role")
    @LoginRequired(role = RoleEnum.ADMIN)
    public Result<String> updateUserRole(@PathVariable("id") Long id, @RequestParam Integer role) {
        userService.updateUserRole(id, role);
        return Result.ok("更新成功", "更新成功");
    }

    /**
     * 用户自助修改密码（需验证旧密码）
     */
    @PutMapping("/password")
    @LoginRequired(role = RoleEnum.USER)
    public Result<String> changePassword(@RequestBody @Valid ChangePasswordRequestVO requestVO) {
        userService.changePassword(requestVO);
        return Result.ok("修改成功", "密码修改成功，请重新登录");
    }

    /**
     * 重置用户密码（管理员）
     */
    @PutMapping("/{id}/password")
    @LoginRequired(role = RoleEnum.ADMIN)
    public Result<String> resetUserPassword(@PathVariable("id") Long id, @RequestBody ResetPasswordRequestVO requestVO) {
        userService.resetUserPassword(id, requestVO.getPassword());
        return Result.ok("重置成功", "重置成功");
    }

    /**
     * 删除用户（管理员）
     */
    @DeleteMapping("/{id}")
    @LoginRequired(role = RoleEnum.ADMIN)
    public Result<String> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return Result.ok("删除成功", "删除成功");
    }
}
