package com.aizuda.snail.ai.admin.service.rag;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.vo.rag.RagChunkResponseVO;
import com.aizuda.snail.ai.admin.vo.rag.RagDocumentImageResponseVO;
import com.aizuda.snail.ai.admin.vo.rag.RagDocumentResponseVO;
import com.aizuda.snail.ai.features.resource.ResourceService;
import com.aizuda.snail.ai.persistence.rag.po.RagChunkPO;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentImagePO;
import com.aizuda.snail.ai.persistence.rag.po.RagDocumentPO;
import com.aizuda.snail.ai.persistence.resource.po.ResourcePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * RAG 文档视图装配器，集中承担文档 / 切片 / 解析图片的 PO → VO 转换与预览、下载链接组装，
 * 不承担任何持久化写操作。
 *
 * @author 唐亚峰 | battcn
 * @since 2026-07-20
 */
@Component
@RequiredArgsConstructor
public class RagDocumentAssembler {

    private static final String DOWNLOAD_QUERY = "download=true";

    private final ResourceService resourceService;

    /**
     * 将文档 PO 转换为响应视图（补充资源文件大小、预览与下载链接）
     *
     * @param po 文档持久化对象
     * @return 文档响应视图
     */
    public RagDocumentResponseVO toResponseVO(RagDocumentPO po) {
        ResourcePO resource = resourceService.getById(po.getResourceId());
        String previewUrl = resource == null ? null : resource.getAccessUrl();

        return RagDocumentResponseVO.builder()
                .id(po.getId())
                .ragId(po.getRagId())
                .name(po.getName())
                .fileType(po.getFileType())
                .sourceType(po.getSourceType())
                .status(po.getStatus())
                .errorMsg(po.getErrorMsg())
                .chunkCount(po.getChunkCount())
                .pageCount(po.getPageCount())
                .elementCount(po.getElementCount())
                .tableCount(po.getTableCount())
                .imageCount(po.getImageCount())
                .parseTime(po.getParseTime())
                .mdContent(po.getMdContent())
                .docMetadata(po.getDocMetadata())
                .fileSize(resource == null ? null : resource.getFileSize())
                .resourceId(po.getResourceId())
                .previewUrl(previewUrl)
                .downloadUrl(buildDownloadUrl(previewUrl))
                .createDt(po.getCreateDt())
                .updateDt(po.getUpdateDt())
                .build();
    }

    /**
     * 将文档解析图片 PO 转换为响应视图
     *
     * @param po 文档解析图片持久化对象
     * @return 图片响应视图
     */
    public RagDocumentImageResponseVO toImageResponseVO(RagDocumentImagePO po) {
        return RagDocumentImageResponseVO.builder()
                .id(po.getId())
                .ragId(po.getRagId())
                .documentId(po.getDocumentId())
                .chunkId(po.getChunkId())
                .resourceId(po.getResourceId())
                .imageIndex(po.getImageIndex())
                .imageUrl(po.getImageUrl())
                .caption(po.getCaption())
                .figureNo(po.getFigureNo())
                .figureTitle(po.getFigureTitle())
                .sectionTitle(po.getSectionTitle())
                .sourcePage(po.getSourcePage())
                .documentName(po.getDocumentName())
                .ocrText(po.getOcrText())
                .createDt(po.getCreateDt())
                .updateDt(po.getUpdateDt())
                .build();
    }

    /**
     * 将切片 PO 转换为响应视图（可携带所属文档名）
     *
     * @param po           切片持久化对象
     * @param documentName 所属文档名（可为空）
     * @return 切片响应视图
     */
    public RagChunkResponseVO toChunkResponseVO(RagChunkPO po, String documentName) {
        return RagChunkResponseVO.builder()
                .id(po.getId())
                .ragId(po.getRagId())
                .documentId(po.getDocumentId())
                .paragraphIndex(po.getParagraphIndex())
                .chunkIndex(po.getChunkIndex())
                .content(po.getContent())
                .tokenCount(po.getTokenCount())
                .vectorId(po.getVectorId())
                .documentName(documentName)
                .createDt(po.getCreateDt())
                .updateDt(po.getUpdateDt())
                .build();
    }

    /**
     * 基于预览链接拼接下载链接（追加 download=true 查询参数）
     */
    private String buildDownloadUrl(String previewUrl) {
        if (StrUtil.isBlank(previewUrl)) {
            return null;
        }
        String separator = previewUrl.contains("?") ? "&" : "?";
        return previewUrl + separator + DOWNLOAD_QUERY;
    }
}
