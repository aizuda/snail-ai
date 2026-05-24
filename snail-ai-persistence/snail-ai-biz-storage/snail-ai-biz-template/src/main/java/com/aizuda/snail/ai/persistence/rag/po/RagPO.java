package com.aizuda.snail.ai.persistence.rag.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG 持久化对象
 * 表: snail_ai_rag
 *
 * 表示一个知识库
 * 知识库由多个文档和向量库组成
 * 支持多种向量库和搜索引擎的灵活配置
 *
 * @author opensnail
 * @date 2026-04-14
 */
@TableName("snail_ai_rag")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RagPO {

    /**
     * 知识库ID (主键)
     * 自增主键，全局唯一
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识库名称
     * 供用户识别的显示名称
     */
    private String name;

    /**
     * 知识库描述
     * 知识库的功能和内容说明
     */
    private String description;

    /**
     * 知识库图标URL
     * 在UI中显示的图标/缩略图
     */
    private String icon;

    /**
     * 向量库实例ID (外键, 可为null)
     * 关联到 snail_ai_store_instance.id
     * 通过此ID可关联查询向量库的类型信息
     */
    private Long vectorStoreInstanceId;

    /**
     * 向量维度（可选）
     * 为空时使用嵌入模型默认维度
     */
    private Integer dimensionOfVectorModel;

    /**
     * 嵌入模型ID (外键)
     * 关联到 snail_ai_model_config.id
     * 用于将文本转换为向量的嵌入模型
     */
    private Long embeddingModelId;

    /**
     * 重排模型ID (外键, 可为null)
     * 关联到 snail_ai_model_config.id
     * 用于对检索结果进行重排
     */
    private Long rerankModelId;

    /**
     * 是否启用搜索引擎
     * true: 启用搜索引擎增强检索
     * false: 仅使用向量检索
     */
    private Boolean searchEngineEnable;

    /**
     * 搜索引擎实例ID (外键, 可为null)
     * 关联到 snail_ai_store_instance.id
     * 通过此ID可关联查询搜索引擎的类型信息
     */
    private Long searchEngineInstanceId;

    /**
     * 分割符
     * 文本分割的分隔符规则
     * 例如: 句号、换行符、特定字符等
     */
    private String delimiter;

    /**
     * RAG 增强配置
     * 用于提升检索效果的增强策略
     */
    private String ragEnhancement;

    /**
     * 配置参数 (JSON格式)
     * 存储知识库的扩展配置
     */
    private String config;

    /**
     * 文档去重策略
     * 0=NONE, 1=BY_NAME, 2=BY_CONTENT, 3=BY_NAME_OR_CONTENT
     */
    private Integer dedupStrategy;

    /**
     * 命中去重时的处理动作
     * 0=REJECT, 1=SKIP, 2=OVERWRITE
     */
    private Integer dedupAction;

    /**
     * 是否在上传前进行二次确认
     * true: 走 preview → commit 两阶段；false: 直传
     */
    private Boolean uploadConfirm;

    /**
     * 创建时间
     * 知识库首次创建的时刻
     */
    private LocalDateTime createDt;

    /**
     * 更新时间
     * 知识库最后一次更新的时刻
     */
    private LocalDateTime updateDt;
}
