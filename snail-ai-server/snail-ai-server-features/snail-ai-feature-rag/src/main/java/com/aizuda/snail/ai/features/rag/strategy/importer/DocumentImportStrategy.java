package com.aizuda.snail.ai.features.rag.strategy.importer;

/**
 * 文档导入策略：按来源类型（上传 / URL / 文本）将原始文档读取为可入库的 {@link ImportResult}。
 *
 * @author opensnail
 * @since 2026-06-14
 */
public interface DocumentImportStrategy {

    /**
     * 是否支持指定来源类型。
     *
     * @param sourceType 来源类型（如 UPLOAD、URL、TEXT）
     * @return 支持返回 true
     */
    boolean supports(String sourceType);

    /**
     * 执行导入，返回文件名、类型、内容哈希与原始字节等结果。
     *
     * @param request 导入请求
     * @return 导入结果
     */
    ImportResult importDocument(ImportRequest request);
}
