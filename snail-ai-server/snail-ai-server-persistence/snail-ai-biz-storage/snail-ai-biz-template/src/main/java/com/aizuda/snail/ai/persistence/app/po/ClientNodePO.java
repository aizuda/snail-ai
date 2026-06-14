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
 * 客户端节点信息持久化对象
 * 表: sai_client_node
 *
 * 记录分布式系统中各个client节点的信息和状态
 * 用于负载均衡、健康检查和资源分配
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("sai_client_node")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientNodePO {

    /**
     * 节点ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 应用ID (外键)
     * 关联到 sai_app.app_id
     * 标识该节点属于哪个应用
     */
    private String appId;

    /**
     * 主机唯一标识符
     * 通常为主机名或UUID，在节点启动时生成
     */
    private String hostId;

    /**
     * 主机IP地址
     * 该client节点所在主机的IP地址
     */
    private String hostIp;

    /**
     * gRPC服务端口
     * 该节点提供gRPC服务的监听端口
     */
    private Integer grpcPort;

    /**
     * 最大并发处理能力
     * 该节点能同时处理的最大对话/请求数
     */
    private Integer maxConcurrent;

    /**
     * 当前活跃的对话数
     * 实时统计，用于负载均衡
     */
    private Integer activeChats;

    /**
     * 支持的模型提供商列表 (JSON格式)
     * 例如: ["openai", "claude", "ollama"]
     * 逗号分隔或JSON数组格式
     */
    private String supportedProviders;

    /**
     * 节点标签 (JSON格式)
     * 自定义标签，可用于节点选择和分类
     * 例如: {"region":"cn-east", "priority":"high"}
     */
    private String labels;

    /**
     * 节点过期时间
     * 节点注册的过期时间，用于心跳检测和自动清理
     * 定期续期则更新此时间
     */
    private LocalDateTime expireDt;

    /**
     * 创建时间
     * 节点首次注册的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * 节点最后一次更新信息的时刻（如心跳、状态更新）
     */
    private LocalDateTime updateDt;
}
