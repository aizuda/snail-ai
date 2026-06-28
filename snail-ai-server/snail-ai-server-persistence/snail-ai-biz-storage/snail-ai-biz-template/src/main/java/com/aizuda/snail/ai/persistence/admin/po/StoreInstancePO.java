package com.aizuda.snail.ai.persistence.admin.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 存储实例持久化对象
 * 表: sai_store_instance
 *
 * 表示一个外部存储服务的配置实例
 * 支持多种存储类型：向量库(PG_VECTOR/Milvus/ES)、文件存储等
 * 同一类型可配置多个实例实现负载均衡或灾备
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("sai_store_instance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreInstancePO {

    /**
     * 实例ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 实例名称
     * 供用户识别的显示名称
     * 例如: "milvus-prod", "pg-vector-backup"
     */
    private String name;

    /**
     * 存储类别
     * 对应 StoreCategoryEnum 枚举值
     * VECTOR_STORE: 向量库
     * FILE_STORE: 文件存储
     * SEARCH_ENGINE: 搜索引擎
     */
    private Integer category;

    /**
     * 存储类型
     * PG_VECTOR: PostgreSQL向量扩展
     * MILVUS: Milvus向量库
     * ELASTICSEARCH: Elasticsearch搜索引擎
     * PG_FULLTEXT: PostgreSQL全文搜索
     * OSS: 对象存储服务
     * S3: AWS S3
     */
    private Integer type;

    /**
     * 实例配置 (JSON格式)
     * 根据type存储相应的连接信息
     * 例如:
     * {
     *   "host": "localhost",
     *   "port": 19530,
     *   "database": "default",
     *   "username": "root",
     *   "password": "Milvus"
     * }
     */
    private String config;

    /**
     * 实例状态
     * ACTIVE: 活跃/可用
     * INACTIVE: 非活跃/禁用
     * ERROR: 错误/连接失败
     * MAINTENANCE: 维护中
     */
    private Integer status;

    /**
     * 是否为默认实例
     * true: 该类型的默认实例，当未指定具体实例时使用
     * false: 非默认实例
     * 每种类型同一时刻仅一个实例为默认
     */
    private Boolean isDefault;

    /**
     * 创建时间
     * 实例配置首次创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * 实例配置最后一次更新的时刻
     */
    private LocalDateTime updateDt;
}
