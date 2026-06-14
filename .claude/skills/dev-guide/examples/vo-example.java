package com.aizuda.snail.ai.admin.vo.user;

import com.aizuda.snail.ai.common.enums.RoleEnum;
import com.aizuda.snail.ai.common.enums.StatusEnum;
import com.aizuda.snail.ai.common.vo.BaseQueryVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * VO 类示例集合
 * 
 * VO (View Object) 用于:
 * - API 响应（ResponseVO）
 * - API 请求（RequestVO）
 * - 查询参数（QueryVO）
 * 
 * @author opensnail
 * @date 2026-04-01
 */

// ========== 1. 响应 VO ==========

/**
 * 用户信息响应 VO
 * 
 * 用于 API 响应,返回给前端的用户信息
 * 
 * 特点:
 * - 使用 @Data 自动生成 getter/setter
 * - 使用 @Builder 支持链式构建
 * - 不包含敏感信息（如密码）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoVO {
    
    /**
     * 用户ID
     */
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
     * 角色
     */
    private RoleEnum role;
    
    /**
     * 状态
     */
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

// ========== 2. 创建请求 VO ==========

/**
 * 用户创建请求 VO
 * 
 * 用于接收创建用户的请求参数
 * 
 * 特点:
 * - 使用验证注解（@NotBlank, @Email, @Size 等）
 * - 不包含自动生成的字段（id, createDt 等）
 */
@Data
class UserCreateRequestVO {
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20")
    private String username;
    
    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20")
    private String password;
}

// ========== 3. 更新请求 VO ==========

/**
 * 用户更新请求 VO
 * 
 * 用于接收更新用户的请求参数
 * 
 * 特点:
 * - 所有字段都是可选的
 * - 只更新传入的非空字段
 */
@Data
class UserUpdateRequestVO {
    
    /**
     * 邮箱（可选）
     */
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 密码（可选）
     */
    @Size(min = 6, max = 20, message = "密码长度 6-20")
    private String password;
}

// ========== 4. 查询 VO ==========

/**
 * 用户查询 VO
 * 
 * 用于接收查询参数
 * 
 * 特点:
 * - 继承 BaseQueryVO（包含 page, size）
 * - 包含查询条件字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
class UserQueryVO extends BaseQueryVO {
    
    /**
     * 关键词（搜索用户名或邮箱）
     */
    private String keyword;
    
    /**
     * 角色过滤
     */
    private RoleEnum role;
    
    /**
     * 状态过滤
     */
    private StatusEnum status;
}

// ========== 5. 统计 VO ==========

/**
 * 用户统计 VO
 * 
 * 用于返回统计数据
 * 
 * 特点:
 * - 包含聚合统计字段
 * - 通常用于仪表板、报表
 */
@Data
@Builder
class UserStatsVO {
    
    /**
     * 总用户数
     */
    private Long totalUsers;
    
    /**
     * 活跃用户数
     */
    private Long activeUsers;
    
    /**
     * 管理员数量
     */
    private Long adminCount;
    
    /**
     * 本月新增用户数
     */
    private Long newUsersThisMonth;
}

// ========== 6. 简化 VO ==========

/**
 * 用户简要信息 VO
 * 
 * 用于列表展示,只包含核心字段
 * 
 * 特点:
 * - 字段较少,减少数据传输量
 * - 用于下拉选择、关联展示等场景
 */
@Data
@Builder
class UserBriefVO {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
}

// ========== 7. 批量操作 VO ==========

/**
 * 批量用户操作请求 VO
 * 
 * 用于批量启用/禁用/删除用户
 */
@Data
class UserBatchOperationVO {
    
    /**
     * 用户ID列表
     */
    @NotBlank(message = "用户ID列表不能为空")
    private List<Long> userIds;
    
    /**
     * 操作类型（enable/disable/delete）
     */
    @NotBlank(message = "操作类型不能为空")
    private String operation;
}

/*
 * VO 类设计要点:
 * 
 * 1. 响应 VO (ResponseVO):
 *    - 用于 API 返回
 *    - 使用 @Builder 便于构建
 *    - 不包含敏感信息
 * 
 * 2. 请求 VO (RequestVO):
 *    - 用于接收前端请求
 *    - 使用验证注解（@NotBlank, @Email 等）
 *    - 分为 CreateRequestVO 和 UpdateRequestVO
 * 
 * 3. 查询 VO (QueryVO):
 *    - 继承 BaseQueryVO（page, size）
 *    - 包含过滤条件
 * 
 * 4. 统计 VO (StatsVO):
 *    - 用于返回聚合数据
 *    - 字段通常是数值类型
 * 
 * 5. 简化 VO (BriefVO):
 *    - 只包含核心字段
 *    - 用于下拉选择等场景
 * 
 * 6. 命名规范:
 *    - 响应: {Name}VO, {Name}ResponseVO
 *    - 请求: {Name}CreateRequestVO, {Name}UpdateRequestVO
 *    - 查询: {Name}QueryVO
 *    - 统计: {Name}StatsVO
 *    - 简化: {Name}BriefVO
 * 
 * 7. 注解使用:
 *    - @Data: getter/setter/toString/equals/hashCode
 *    - @Builder: 链式构建
 *    - @NotBlank: 非空验证
 *    - @Email: 邮箱格式验证
 *    - @Size: 长度限制
 * 
 * 文件位置参考:
 * /snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/vo/user/
 * /snail-ai-admin/src/main/java/com/aizuda/snail/ai/admin/vo/memory/
 */
