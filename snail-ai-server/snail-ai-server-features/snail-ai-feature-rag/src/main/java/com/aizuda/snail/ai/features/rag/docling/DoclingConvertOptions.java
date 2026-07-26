package com.aizuda.snail.ai.features.rag.docling;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.persistence.rag.dataobject.RagConfigDO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Docling 转换参数。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@Data
@Builder
public class DoclingConvertOptions {

    public static final String DEFAULT_IMAGE_EXPORT_MODE = "embedded";
    public static final String DEFAULT_PDF_BACKEND = "dlparse_v4";
    public static final String DEFAULT_OCR_LANG = "en";

    /**
     * 是否启用 OCR，适用于扫描 PDF 或图片型页面。
     */
    private boolean doOcr;

    /**
     * 是否启用表格结构识别，开启后 JSON 结果中会包含 tables 节点。
     */
    private boolean doTableStructure;

    /**
     * 图片导出模式。默认 embedded，用于从 JSON/Markdown 中提取图片。
     */
    private String imageExportMode;

    /**
     * OCR 语言列表，Docling Serve 侧按该参数识别文本。
     */
    private List<String> ocrLang;

    /**
     * PDF 后端实现名称，保留为配置项便于后续升级 Docling 后端。
     */
    private String pdfBackend;

    /**
     * 是否将 Docling 返回的图片保存到资源库和 sai_rag_document_image。
     */
    private boolean saveImages;

    public static DoclingConvertOptions fromConfig(RagConfigDO.DoclingParams params) {
        if (params == null) {
            return defaults();
        }
        return DoclingConvertOptions.builder()
                .doOcr(params.getDoOcr() == null || Boolean.TRUE.equals(params.getDoOcr()))
                .doTableStructure(params.getDoTableStructure() == null || Boolean.TRUE.equals(params.getDoTableStructure()))
                .imageExportMode(StrUtil.blankToDefault(params.getImageExportMode(), DEFAULT_IMAGE_EXPORT_MODE))
                .ocrLang(CollUtil.isNotEmpty(params.getOcrLang()) ? params.getOcrLang() : List.of(DEFAULT_OCR_LANG))
                .pdfBackend(StrUtil.blankToDefault(params.getPdfBackend(), DEFAULT_PDF_BACKEND))
                .saveImages(params.getSaveImages() == null || Boolean.TRUE.equals(params.getSaveImages()))
                .build();
    }

    public static DoclingConvertOptions defaults() {
        return DoclingConvertOptions.builder()
                .doOcr(true)
                .doTableStructure(true)
                .imageExportMode(DEFAULT_IMAGE_EXPORT_MODE)
                .ocrLang(List.of(DEFAULT_OCR_LANG))
                .pdfBackend(DEFAULT_PDF_BACKEND)
                .saveImages(true)
                .build();
    }
}
