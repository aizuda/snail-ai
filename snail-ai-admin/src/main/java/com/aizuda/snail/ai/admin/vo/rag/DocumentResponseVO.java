package com.aizuda.snail.ai.admin.vo.rag;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author opensnail
 * @date 2025-07-19
 */
@Data
public class DocumentResponseVO {

    private Long id;

    private String resource;

    private String prefix;

    private String suffix;

    private String content;

    private int status;

    private LocalDateTime createDt;

    private LocalDateTime updateDt;
}
