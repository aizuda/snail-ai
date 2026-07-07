package com.aizuda.snail.ai.feature.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体对话附件传输对象。
 *
 * @author opensnail
 * @since 2026-07-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatAttachmentDTO {

    /** 资源 ID */
    private Long resourceId;

    /** 附件类型 */
    private String type;

    /** 文件名 */
    private String fileName;

    /** MIME 类型 */
    private String mimeType;

    /** 文件大小 */
    private Long size;

    /** 访问地址 */
    private String accessUrl;

    /** 分发给模型客户端使用的 Base64 内容，不落库 */
    private String base64;
}
