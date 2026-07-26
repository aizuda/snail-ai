package com.aizuda.snail.ai.feature.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能体对话记录扩展元数据。
 *
 * @author opensnail
 * @since 2026-07-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatRecordMetadataDTO {

    /** 对话记录关联的附件列表 */
    private List<Attachment> attachments;

    /**
     * 对话记录附件元数据。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {

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
    }
}
