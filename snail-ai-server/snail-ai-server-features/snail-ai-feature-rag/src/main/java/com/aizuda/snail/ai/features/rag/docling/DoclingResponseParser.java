package com.aizuda.snail.ai.features.rag.docling;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Docling 服务响应解析器：将 Docling Serve 返回的 JSON 解析为结构化元素（标题/段落/表格/图片/公式），
 * 同时提取图片 base64、图注与 OCR 文本。兼容 v2 的 {@code json_content} 结构与旧版 {@code elements} 结构。
 *
 * @author opensnail
 * @author 唐亚峰 | battcn
 * @since 2026-06-30
 */
@Slf4j
@Component
public class DoclingResponseParser {

    private static final String DOCUMENT_FIELD = "document";
    private static final String JSON_CONTENT_FIELD = "json_content";
    private static final String MARKDOWN_CONTENT_FIELD = "md_content";
    private static final String TYPE_TITLE = "title";
    private static final String TYPE_PARAGRAPH = "paragraph";
    private static final String TYPE_TABLE = "table";
    private static final String TYPE_PICTURE = "picture";
    private static final String TYPE_FORMULA = "formula";
    private static final String REF_PREFIX = "#/";
    private static final String IMAGE_PLACEHOLDER = "[image]";
    private static final String IMAGE_CAPTION_PREFIX = "[图片说明: ";
    private static final String IMAGE_CAPTION_SUFFIX = "]";
    private static final String TEXT_SEPARATOR = "\n";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LEVEL = 0;
    private static final Pattern FIGURE_CAPTION_PATTERN = Pattern.compile(
            "^((?:(?:图表|图|Figure|Fig\\.)\\s*)"
                    + "[0-9一二三四五六七八九十百千万]+)\\s*(.*)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * 解析 Docling 响应 JSON
     *
     * @param json   Docling Serve result 接口返回的原始 JSON
     * @param costMs 本次远程解析耗时（毫秒），回填到结果中
     * @return 解析结果；JSON 非法或缺少 document 字段时返回 fail 结果，不抛异常
     */
    public DoclingParseResult parse(String json, long costMs) {
        // Docling Serve result 接口返回 document 包装对象，缺失时视为不可用响应。
        JSONObject root;
        try {
            root = JSONUtil.parseObj(json);
        } catch (Exception e) {
            log.warn("Docling 响应 JSON 解析失败: {}", e.getMessage());
            return DoclingParseResult.fail("Docling response json is invalid");
        }
        JSONObject document = root.getJSONObject(DOCUMENT_FIELD);
        if (document == null) {
            log.warn("Docling 响应缺少 document 字段, keys={}", root.keySet());
            return DoclingParseResult.fail("Docling response missing document field");
        }

        DoclingParseResult result = DoclingParseResult.builder()
                .success(true)
                .markdownContent(document.getStr(MARKDOWN_CONTENT_FIELD, ""))
                .parseTime((int) costMs)
                .elements(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        JSONObject jsonContent = document.getJSONObject(JSON_CONTENT_FIELD);
        if (jsonContent != null) {
            parseJsonContent(jsonContent, result);
        } else {
            // 无 json_content 时走旧版 elements 结构，保证对老版本 Docling 服务的兼容
            parseLegacyElements(document, result);
        }
        log.debug("Docling 解析完成, elements={}, tables={}, images={}, pages={}, costMs={}",
                result.getElementCount(), result.getTableCount(), result.getImageCount(),
                result.getPageCount(), costMs);
        return result;
    }

    /**
     * 将解析结果重组为干净的 Markdown：保留标题层级与公式，图片以「图片说明」文本占位，
     * 并清理 base64 内联图片，供后续切片与向量化使用
     *
     * @param result 解析结果
     * @return 重组后的 Markdown 文本；解析失败时返回空串
     */
    public String toCleanMarkdown(DoclingParseResult result) {
        if (result == null || !result.isSuccess()) {
            return "";
        }
        if (CollUtil.isEmpty(result.getElements())) {
            return DoclingMarkdownSanitizer.cleanBase64Images(result.getMarkdownContent());
        }

        StringBuilder builder = new StringBuilder();
        for (DoclingParseResult.DoclingElement element : result.getElements()) {
            String text = element.getText();
            if (StrUtil.isBlank(text) || IMAGE_PLACEHOLDER.equals(text.trim())) {
                continue;
            }
            if (TYPE_TITLE.equals(element.getType())) {
                // 保留标题层级，便于后续切片继续利用文档结构。
                int level = element.getLevel() != null && element.getLevel() > 0 ? element.getLevel() : 1;
                builder.append("#".repeat(Math.min(level, 6))).append(" ").append(text.trim()).append("\n\n");
                continue;
            }
            if (TYPE_PICTURE.equals(element.getType())) {
                // 图片元素不跳过，将图注和 OCR 文字注入 chunk，使其可被检索。
                builder.append(IMAGE_CAPTION_PREFIX).append(text.trim()).append(IMAGE_CAPTION_SUFFIX).append("\n\n");
                continue;
            }
            if (TYPE_FORMULA.equals(element.getType())) {
                // 公式保留 LaTeX 文本，可直接被检索。
                builder.append(text.trim()).append("\n\n");
                continue;
            }
            builder.append(text.trim()).append("\n\n");
        }
        return DoclingMarkdownSanitizer.cleanBase64Images(builder.toString());
    }

    private void parseJsonContent(JSONObject jsonContent, DoclingParseResult result) {
        JSONObject pages = jsonContent.getJSONObject("pages");
        if (pages != null) {
            result.setPageCount(pages.size());
        }

        JSONArray texts = jsonContent.getJSONArray("texts");
        JSONArray pictures = jsonContent.getJSONArray("pictures");
        JSONArray tables = jsonContent.getJSONArray("tables");
        JSONArray groups = jsonContent.getJSONArray("groups");
        JSONObject body = jsonContent.getJSONObject("body");
        JSONArray children = body != null ? body.getJSONArray("children") : null;
        if (children == null) {
            applyCounts(result, 0);
            return;
        }

        ResolveState state = new ResolveState();
        ResolveSources sources = new ResolveSources(texts, pictures, tables, groups);
        resolveChildren(children, sources, result, state);

        JSONObject origin = jsonContent.getJSONObject("origin");
        if (origin != null) {
            result.setDocMetadata(origin.toString());
        }
        applyCounts(result, state.tableCount);
    }

    private void resolveChildren(JSONArray children, ResolveSources sources, DoclingParseResult result, ResolveState state) {
        for (int i = 0; i < children.size(); i++) {
            JSONObject childRef = children.getJSONObject(i);
            RefParts refParts = parseRef(childRef.getStr("$ref"));
            if (refParts == null) {
                continue;
            }

            if ("groups".equals(refParts.category) && sources.groups != null && refParts.index < sources.groups.size()) {
                JSONObject group = sources.groups.getJSONObject(refParts.index);
                JSONArray groupChildren = group.getJSONArray("children");
                if (groupChildren != null) {
                    resolveChildren(groupChildren, sources, result, state);
                }
                continue;
            }
            if ("texts".equals(refParts.category)) {
                addTextElement(sources.texts, refParts.index, result, state);
                continue;
            }
            if ("tables".equals(refParts.category)) {
                addTableElement(sources.tables, refParts.index, result, state);
                continue;
            }
            if ("pictures".equals(refParts.category)) {
                LookaheadCaption resolvedLookaheadCaption = lookaheadCaption(children, i + 1, sources.texts);
                addPictureElement(sources.pictures, refParts.index, result, state, resolvedLookaheadCaption);
                if (resolvedLookaheadCaption.hasText()) {
                    i++;
                }
            }
        }
    }

    private RefParts parseRef(String ref) {
        if (StrUtil.isBlank(ref) || !ref.startsWith(REF_PREFIX)) {
            return null;
        }
        String[] parts = ref.substring(REF_PREFIX.length()).split("/");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new RefParts(parts[0], Integer.parseInt(parts[1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void addTextElement(JSONArray texts, int refIndex, DoclingParseResult result, ResolveState state) {
        if (texts == null || refIndex >= texts.size()) {
            return;
        }
        JSONObject textObj = texts.getJSONObject(refIndex);
        String label = textObj.getStr("label", "text");
        String type = mapLabelToType(label);
        String text = textObj.getStr("text", "");
        int page = extractPage(textObj);
        result.getElements().add(DoclingParseResult.DoclingElement.builder()
                .index(state.nextIndex())
                .type(type)
                .text(text)
                .page(page)
                .level(textObj.getInt("level", TYPE_TITLE.equals(type) ? 1 : DEFAULT_LEVEL))
                .build());
        state.currentPage = page;
        if (TYPE_TITLE.equals(type) && StrUtil.isNotBlank(text)) {
            state.currentSectionTitle = text.trim();
        }
    }

    private void addTableElement(JSONArray tables, int refIndex, DoclingParseResult result, ResolveState state) {
        if (tables == null || refIndex >= tables.size()) {
            return;
        }
        JSONObject tableObj = tables.getJSONObject(refIndex);
        int page = extractPage(tableObj);
        result.getElements().add(DoclingParseResult.DoclingElement.builder()
                .index(state.nextIndex())
                .type(TYPE_TABLE)
                .text(buildTableText(tableObj))
                .page(page)
                .level(DEFAULT_LEVEL)
                .build());
        state.currentPage = page;
        state.tableCount++;
    }

    /**
     * 前瞻查找紧跟在 picture 引用后面的 text 元素，提取图注。
     * Docling v2 通常将图注放在 picture 后面的 text 中，而不是 picture.captions。
     */
    private LookaheadCaption lookaheadCaption(JSONArray children, int nextIndex, JSONArray texts) {
        if (children == null || nextIndex >= children.size() || texts == null) {
            return LookaheadCaption.empty();
        }
        JSONObject nextRef = children.getJSONObject(nextIndex);
        RefParts nextParts = parseRef(nextRef.getStr("$ref"));
        if (nextParts == null || !"texts".equals(nextParts.category) || nextParts.index >= texts.size()) {
            return LookaheadCaption.empty();
        }
        JSONObject textObj = texts.getJSONObject(nextParts.index);
        String label = textObj.getStr("label", "");
        String text = textObj.getStr("text", "");
        if (StrUtil.isBlank(text)) {
            return LookaheadCaption.empty();
        }
        // 匹配图注前缀，例如“图 X”、“图表 X”、“Figure X”、“Fig. X”。
        String trimmedText = text.trim();
        if (isFigureCaptionText(trimmedText)) {
            return new LookaheadCaption(trimmedText, extractPage(textObj));
        }
        // section_header / title 不是图注。
        if ("title".equals(label) || "section_header".equals(label)) {
            return LookaheadCaption.empty();
        }
        return LookaheadCaption.empty();
    }

    private boolean isFigureCaptionText(String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        String lowerText = text.toLowerCase();
        return text.startsWith("图") || lowerText.startsWith("figure") || lowerText.startsWith("fig.");
    }

    private void addPictureElement(JSONArray pictures, int refIndex, DoclingParseResult result, ResolveState state) {
        addPictureElement(pictures, refIndex, result, state, LookaheadCaption.empty());
    }

    private void addPictureElement(JSONArray pictures, int refIndex, DoclingParseResult result, ResolveState state,
                                   LookaheadCaption lookaheadCaption) {
        if (pictures == null || refIndex >= pictures.size()) {
            return;
        }
        JSONObject pictureObj = pictures.getJSONObject(refIndex);
        int page = resolvePicturePage(pictureObj, lookaheadCaption, state);
        // Docling v2 中 picture.captions 通常为空，优先使用前瞻查到的图注。
        String caption = lookaheadCaption.hasText()
                ? lookaheadCaption.text()
                : extractCaption(pictureObj);
        FigureCaption figureCaption = parseFigureCaption(caption);
        String ocrText = extractPictureOcrText(pictureObj);
        // 将 caption 和 OCR 文字合并为 element.text，确保两者都能进入 chunk 被检索。
        String pictureText;
        if (StrUtil.isNotBlank(caption) && StrUtil.isNotBlank(ocrText)) {
            pictureText = caption + TEXT_SEPARATOR + ocrText;
        } else if (StrUtil.isNotBlank(caption)) {
            pictureText = caption;
        } else if (StrUtil.isNotBlank(ocrText)) {
            pictureText = ocrText;
        } else {
            pictureText = IMAGE_PLACEHOLDER;
        }
        int elementIndex = state.nextIndex();
        result.getElements().add(DoclingParseResult.DoclingElement.builder()
                .index(elementIndex)
                .type(TYPE_PICTURE)
                .text(pictureText)
                .page(page)
                .level(DEFAULT_LEVEL)
                .build());

        String base64 = extractImageBase64(pictureObj.getJSONObject("image"));
        if (StrUtil.isBlank(base64)) {
            return;
        }
        result.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64(base64)
                .caption(caption)
                .figureNo(figureCaption.figureNo())
                .figureTitle(figureCaption.figureTitle())
                .sectionTitle(state.currentSectionTitle)
                .elementIndex(elementIndex)
                .page(page)
                .ocrText(ocrText)
                .build());
    }

    private FigureCaption parseFigureCaption(String caption) {
        if (StrUtil.isBlank(caption)) {
            return new FigureCaption("", "");
        }
        Matcher matcher = FIGURE_CAPTION_PATTERN.matcher(caption.trim());
        if (!matcher.matches()) {
            return new FigureCaption("", "");
        }
        return new FigureCaption(matcher.group(1).trim(), StrUtil.trimToEmpty(matcher.group(2)));
    }

    private String buildTableText(JSONObject tableObj) {
        JSONObject data = tableObj.getJSONObject("data");
        if (data == null) {
            return "[table]";
        }
        JSONArray cells = data.getJSONArray("table_cells");
        int numCols = data.getInt("num_cols", 0);
        int numRows = data.getInt("num_rows", 0);
        if (cells == null || cells.isEmpty() || numCols <= 0 || numRows <= 0) {
            return "[table]";
        }

        String[][] grid = new String[numRows][numCols];
        for (int i = 0; i < cells.size(); i++) {
            JSONObject cell = cells.getJSONObject(i);
            int row = cell.getInt("start_row_offset_idx", 0);
            int col = cell.getInt("start_col_offset_idx", 0);
            if (row >= 0 && row < numRows && col >= 0 && col < numCols) {
                grid[row][col] = cell.getStr("text", "");
            }
        }

        StringBuilder builder = new StringBuilder();
        for (int row = 0; row < numRows; row++) {
            builder.append("|");
            for (int col = 0; col < numCols; col++) {
                builder.append(" ").append(StrUtil.blankToDefault(grid[row][col], "")).append(" |");
            }
            if (row == 0) {
                builder.append("\n|");
                for (int col = 0; col < numCols; col++) {
                    builder.append("---|");
                }
            }
            if (row < numRows - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private void parseLegacyElements(JSONObject document, DoclingParseResult result) {
        JSONObject metadata = document.getJSONObject("metadata");
        if (metadata != null) {
            result.setPageCount(metadata.getInt("page_count", 0));
            result.setDocMetadata(metadata.toString());
        }

        JSONArray elements = document.getJSONArray("elements");
        if (elements == null) {
            applyCounts(result, 0);
            return;
        }
        int tableCount = 0;
        String currentSectionTitle = "";
        for (int i = 0; i < elements.size(); i++) {
            JSONObject elementObj = elements.getJSONObject(i);
            String type = elementObj.getStr("type", TYPE_PARAGRAPH);
            String text = elementObj.getStr("text", "");
            result.getElements().add(DoclingParseResult.DoclingElement.builder()
                    .index(i)
                    .type(type)
                    .text(text)
                    .page(elementObj.getInt("page", DEFAULT_PAGE))
                    .level(elementObj.getInt("level", DEFAULT_LEVEL))
                    .build());
            if (TYPE_TITLE.equals(type) && StrUtil.isNotBlank(text)) {
                currentSectionTitle = text.trim();
            }
            if (TYPE_TABLE.equals(type)) {
                tableCount++;
            }
            if (TYPE_PICTURE.equals(type) && StrUtil.isNotBlank(elementObj.getStr("image"))) {
                String caption = elementObj.getStr("caption", "");
                FigureCaption figureCaption = parseFigureCaption(caption);
                result.getImages().add(DoclingParseResult.DoclingImage.builder()
                        .base64(stripBase64Prefix(elementObj.getStr("image")))
                        .caption(caption)
                        .figureNo(figureCaption.figureNo())
                        .figureTitle(figureCaption.figureTitle())
                        .sectionTitle(currentSectionTitle)
                        .elementIndex(i)
                        .page(elementObj.getInt("page", DEFAULT_PAGE))
                        .ocrText(elementObj.getStr("ocr_text", ""))
                        .build());
            }
        }
        applyCounts(result, tableCount);
    }

    private void applyCounts(DoclingParseResult result, int tableCount) {
        result.setElementCount(result.getElements() == null ? 0 : result.getElements().size());
        result.setTableCount(tableCount);
        result.setImageCount(result.getImages() == null ? 0 : result.getImages().size());
    }

    private String mapLabelToType(String label) {
        return switch (label) {
            case "title", "section_header" -> TYPE_TITLE;
            case "list_item" -> "list";
            case "caption" -> TYPE_PARAGRAPH;
            case "formula", "equation" -> TYPE_FORMULA;
            default -> TYPE_PARAGRAPH;
        };
    }

    private int resolvePicturePage(JSONObject pictureObj, LookaheadCaption lookaheadCaption, ResolveState state) {
        Integer picturePage = extractPageOrNull(pictureObj);
        if (picturePage != null) {
            return picturePage;
        }
        if (lookaheadCaption.page() != null && lookaheadCaption.page() > 0) {
            return lookaheadCaption.page();
        }
        if (state.currentPage != null && state.currentPage > 0) {
            return state.currentPage;
        }
        return DEFAULT_PAGE;
    }

    private int extractPage(JSONObject element) {
        Integer page = extractPageOrNull(element);
        return page == null ? DEFAULT_PAGE : page;
    }

    private Integer extractPageOrNull(JSONObject element) {
        if (element == null) {
            return null;
        }
        JSONArray prov = element.getJSONArray("prov");
        if (prov != null && !prov.isEmpty()) {
            JSONObject first = prov.getJSONObject(0);
            int pageNo = first.getInt("page_no", -1);
            if (pageNo > 0) {
                return pageNo;
            }
        }
        // Docling v2 在 prov 为空时，可能把页码放在 annotations 中。
        JSONArray annotations = element.getJSONArray("annotations");
        if (annotations != null && !annotations.isEmpty()) {
            for (int i = 0; i < annotations.size(); i++) {
                JSONObject ann = annotations.getJSONObject(i);
                int pageNo = ann.getInt("page_no", -1);
                if (pageNo > 0) {
                    return pageNo;
                }
            }
        }
        return null;
    }

    private String extractCaption(JSONObject element) {
        JSONArray captions = element.getJSONArray("captions");
        if (captions == null || captions.isEmpty()) {
            return "";
        }
        JSONObject first = captions.getJSONObject(0);
        return first == null ? "" : first.getStr("text", "");
    }

    /**
     * 从 picture.annotations 数组中提取 OCR 文本。
     * Docling 会将 OCR 结果放在 annotations[].text 中，多个 annotation 使用空格拼接。
     */
    private String extractPictureOcrText(JSONObject pictureObj) {
        JSONArray annotations = pictureObj.getJSONArray("annotations");
        if (annotations == null || annotations.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < annotations.size(); i++) {
            JSONObject ann = annotations.getJSONObject(i);
            String text = ann.getStr("text", "");
            if (StrUtil.isBlank(text)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(text.trim());
        }
        return sb.toString();
    }

    private String extractImageBase64(JSONObject imageObj) {
        if (imageObj == null) {
            return "";
        }
        String uri = imageObj.getStr("uri", "");
        return StrUtil.isBlank(uri) ? "" : stripBase64Prefix(uri);
    }

    private String stripBase64Prefix(String uri) {
        if (uri == null) {
            return "";
        }
        int commaIndex = uri.indexOf(',');
        String base64 = commaIndex >= 0 ? uri.substring(commaIndex + 1) : uri;
        return base64.replaceAll("\\s+", "");
    }

    private record ResolveSources(JSONArray texts, JSONArray pictures, JSONArray tables, JSONArray groups) {
    }

    private record RefParts(String category, int index) {
    }

    private record FigureCaption(String figureNo, String figureTitle) {
    }

    private record LookaheadCaption(String text, Integer page) {
        private static LookaheadCaption empty() {
            return new LookaheadCaption("", null);
        }

        private boolean hasText() {
            return StrUtil.isNotBlank(text);
        }
    }

    private static class ResolveState {
        private int index;
        private int tableCount;
        private String currentSectionTitle;
        private Integer currentPage;

        private int nextIndex() {
            return index++;
        }
    }
}
