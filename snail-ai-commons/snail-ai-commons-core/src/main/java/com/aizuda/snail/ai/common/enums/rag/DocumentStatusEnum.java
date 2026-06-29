package com.aizuda.snail.ai.common.enums.rag;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 知识库文档处理状态枚举，标识文档从入库到解析完成的生命周期阶段。
 *
 * @author opensnail
 * @since 2026-05-05
 */
@Getter
@AllArgsConstructor
public enum DocumentStatusEnum {

    PENDING(0, "待处理"),
    PARSING(1, "解析中"),
    PROCESSING(2, "处理中"),
    SUCCESS(3, "处理完成"),
    FAILED(4, "处理失败");

    /**
     * 持久化存储值（@EnumValue 映射数据库字段）
     */
    @EnumValue
    private final Integer status;
    /**
     * 枚举描述（中文）
     */
    private final String description;
}
