package com.aizuda.snail.ai.admin.vo.rag;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * RAG 文档响应：文档内容、解析状态及创建 / 更新时间。
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
