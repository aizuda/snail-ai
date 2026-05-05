package com.aizuda.snail.ai.features.rag.strategy.parser;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class HtmlParser implements DocumentParser {

    @Override
    public boolean supports(String fileType) {
        return "html".equalsIgnoreCase(fileType) || "htm".equalsIgnoreCase(fileType);
    }

    @Override
    public String parse(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String html = reader.lines().collect(Collectors.joining("\n"));
            return stripHtmlTags(html);
        } catch (Exception e) {
            throw new SnailAiException("Failed to parse HTML", e);
        }
    }

    private String stripHtmlTags(String html) {
        String text = html.replaceAll("(?i)<script[^>]*>[\\s\\S]*?</script>", "");
        text = text.replaceAll("(?i)<style[^>]*>[\\s\\S]*?</style>", "");
        // 块级标签替换为换行，保留段落结构
        text = text.replaceAll("(?i)</(p|div|h[1-6]|li|tr|blockquote|section|article)\\s*>", "\n");
        text = text.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("<[^>]+>", " ");
        // HTML 实体解码
        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&quot;", "\"");
        text = text.replaceAll("&apos;", "'");
        text = decodeNumericEntities(text);
        // 行内多余空格合并，但保留换行
        text = text.replaceAll("[ \\t]+", " ");
        // 合并连续空行为单个换行
        text = text.replaceAll("\\n[ \\t]*\\n+", "\n\n");
        return text.trim();
    }

    private String decodeNumericEntities(String text) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '&' && i + 2 < text.length() && text.charAt(i + 1) == '#') {
                int end = text.indexOf(';', i + 2);
                if (end != -1 && end - i < 10) {
                    String num = text.substring(i + 2, end);
                    try {
                        int code = Integer.parseInt(num);
                        sb.append((char) code);
                        i = end + 1;
                        continue;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            sb.append(text.charAt(i));
            i++;
        }
        return sb.toString();
    }
}
