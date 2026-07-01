package com.aizuda.snail.ai.features.rag.docling;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Docling 服务响应解析器。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@Component
public class DoclingResponseParser {

    private static final String DOCUMENT_FIELD = "document";
    private static final String JSON_CONTENT_FIELD = "json_content";
    private static final String MARKDOWN_CONTENT_FIELD = "md_content";
    private static final String TYPE_TITLE = "title";
    private static final String TYPE_PARAGRAPH = "paragraph";
    private static final String TYPE_TABLE = "table";
    private static final String TYPE_PICTURE = "picture";
    private static final String REF_PREFIX = "#/";
    private static final String IMAGE_PLACEHOLDER = "[image]";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LEVEL = 0;

    public DoclingParseResult parse(String json, long costMs) {
        // Docling Serve result 接口返回 document 包装对象，缺失时视为不可用响应。
        JSONObject root;
        try {
            root = JSONUtil.parseObj(json);
        } catch (Exception e) {
            return DoclingParseResult.fail("Docling response json is invalid");
        }
        JSONObject document = root.getJSONObject(DOCUMENT_FIELD);
        if (document == null) {
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
            return result;
        }
        parseLegacyElements(document, result);
        return result;
    }

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
                // 保留标题层级，便于后续切片仍能利用文档结构。
                int level = element.getLevel() != null && element.getLevel() > 0 ? element.getLevel() : 1;
                builder.append("#".repeat(Math.min(level, 6))).append(" ").append(text.trim()).append("\n\n");
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
        ResolveSources sources = new ResolveSources(texts, pictures, tables, groups, pages);
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
                addPictureElement(sources.pictures, sources.pages, refParts.index, result, state);
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
        result.getElements().add(DoclingParseResult.DoclingElement.builder()
                .index(state.nextIndex())
                .type(type)
                .text(textObj.getStr("text", ""))
                .page(extractPage(textObj))
                .level(textObj.getInt("level", TYPE_TITLE.equals(type) ? 1 : DEFAULT_LEVEL))
                .build());
    }

    private void addTableElement(JSONArray tables, int refIndex, DoclingParseResult result, ResolveState state) {
        if (tables == null || refIndex >= tables.size()) {
            return;
        }
        JSONObject tableObj = tables.getJSONObject(refIndex);
        result.getElements().add(DoclingParseResult.DoclingElement.builder()
                .index(state.nextIndex())
                .type(TYPE_TABLE)
                .text(buildTableText(tableObj))
                .page(extractPage(tableObj))
                .level(DEFAULT_LEVEL)
                .build());
        state.tableCount++;
    }

    private void addPictureElement(JSONArray pictures, JSONObject pages, int refIndex,
                                   DoclingParseResult result, ResolveState state) {
        if (pictures == null || refIndex >= pictures.size()) {
            return;
        }
        JSONObject pictureObj = pictures.getJSONObject(refIndex);
        int page = extractPage(pictureObj);
        String caption = extractCaption(pictureObj);
        result.getElements().add(DoclingParseResult.DoclingElement.builder()
                .index(state.nextIndex())
                .type(TYPE_PICTURE)
                .text(StrUtil.isNotBlank(caption) ? caption : IMAGE_PLACEHOLDER)
                .page(page)
                .level(DEFAULT_LEVEL)
                .build());

        JSONObject pictureImage = pictureObj.getJSONObject("image");
        String base64 = extractImageBase64(pictureImage);
        if (StrUtil.isBlank(base64) && pages != null && state.collectedPageImages.add(page)) {
            base64 = extractPageImage(pages, page);
        }
        if (StrUtil.isBlank(base64)) {
            return;
        }
        result.getImages().add(DoclingParseResult.DoclingImage.builder()
                .base64(base64)
                .caption(caption)
                .page(page)
                .ocrText("")
                .build());
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
        for (int i = 0; i < elements.size(); i++) {
            JSONObject elementObj = elements.getJSONObject(i);
            String type = elementObj.getStr("type", TYPE_PARAGRAPH);
            result.getElements().add(DoclingParseResult.DoclingElement.builder()
                    .index(i)
                    .type(type)
                    .text(elementObj.getStr("text", ""))
                    .page(elementObj.getInt("page", DEFAULT_PAGE))
                    .level(elementObj.getInt("level", DEFAULT_LEVEL))
                    .build());
            if (TYPE_TABLE.equals(type)) {
                tableCount++;
            }
            if (TYPE_PICTURE.equals(type) && StrUtil.isNotBlank(elementObj.getStr("image"))) {
                result.getImages().add(DoclingParseResult.DoclingImage.builder()
                        .base64(stripBase64Prefix(elementObj.getStr("image")))
                        .caption(elementObj.getStr("caption", ""))
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
            default -> TYPE_PARAGRAPH;
        };
    }

    private int extractPage(JSONObject element) {
        JSONArray prov = element.getJSONArray("prov");
        if (prov != null && !prov.isEmpty()) {
            JSONObject first = prov.getJSONObject(0);
            return first.getInt("page_no", DEFAULT_PAGE);
        }
        return DEFAULT_PAGE;
    }

    private String extractCaption(JSONObject element) {
        JSONArray captions = element.getJSONArray("captions");
        if (captions == null || captions.isEmpty()) {
            return "";
        }
        JSONObject first = captions.getJSONObject(0);
        return first == null ? "" : first.getStr("text", "");
    }

    private String extractPageImage(JSONObject pages, int page) {
        JSONObject pageObj = pages.getJSONObject(String.valueOf(page));
        if (pageObj == null) {
            return "";
        }
        return extractImageBase64(pageObj.getJSONObject("image"));
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

    private record ResolveSources(JSONArray texts, JSONArray pictures, JSONArray tables, JSONArray groups, JSONObject pages) {
    }

    private record RefParts(String category, int index) {
    }

    private static class ResolveState {
        private int index;
        private int tableCount;
        private final Set<Integer> collectedPageImages = new HashSet<>();

        private int nextIndex() {
            return index++;
        }
    }
}
