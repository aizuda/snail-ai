package com.aizuda.snail.ai.features.rag.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 命中去重时的处理动作
 *
 * @author opensnail
 */
@Getter
@AllArgsConstructor
public enum DedupAction {

    /** 拒收并报错 */
    REJECT(0),

    /** 跳过本次上传，不报错 */
    SKIP(1),

    /** 删除旧文档（含 chunks/向量），按新文件重新处理 */
    OVERWRITE(2);

    @EnumValue
    private final Integer code;

    public static DedupAction fromCode(Integer code) {
        if (code == null) {
            return REJECT;
        }
        for (DedupAction e : values()) {
            if (Objects.equals(e.code, code)) {
                return e;
            }
        }
        return REJECT;
    }
}
