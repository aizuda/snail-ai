package com.aizuda.snail.ai.common.enums.memory;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemoryEventEnum {

    ADD(1, "新增"),
    UPDATE(2, "更新"),
    DELETE(3, "删除"),
    NOOP(4, "无操作");

    @EnumValue
    private final Integer event;
    private final String description;

    public static MemoryEventEnum fromEvent(Integer event) {
        if (event == null) {
            return null;
        }
        for (MemoryEventEnum e : values()) {
            if (e.event.equals(event)) {
                return e;
            }
        }
        return null;
    }

    public static MemoryEventEnum fromLegacy(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toUpperCase()) {
            case "ADD" -> ADD;
            case "UPDATE" -> UPDATE;
            case "DELETE" -> DELETE;
            case "NOOP" -> NOOP;
            default -> null;
        };
    }
}
