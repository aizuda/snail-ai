package com.aizuda.snail.ai.persistence.app.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * App应用信息持久化对象
 * 表: sai_app
 *
 * 用于管理和隔离不同应用的配置、数据、Agent等资源
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("sai_app")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppPO {

    /**
     * 应用ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 应用唯一标识符
     * 用于API调用时识别应用，通常为UUID或自定义唯一字符串
     */
    private String appId;

    /**
     * 应用名称
     * 供管理员或用户识别的应用显示名称
     */
    private String appName;

    /**
     * 应用描述
     * 应用功能和用途的详细说明
     */
    private String description;

    /**
     * 应用访问令牌 (加密存储)
     * 用于API请求的身份认证
     */
    private String token;

    /**
     * 路由策略
     * 定义该应用的Agent、数据、模型等资源如何被分发和访问
     * 可取值：LOCAL (本地), CLUSTER (集群), HYBRID (混合)等
     */
    private String routeStrategy;

    /**
     * 应用状态
     * 0: 禁用/停用
     * 1: 启用/活跃
     */
    private Integer status;

    /**
     * 创建时间
     * 记录应用首次创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * 记录应用最后一次更新的时刻
     */
    private LocalDateTime updateDt;
}
