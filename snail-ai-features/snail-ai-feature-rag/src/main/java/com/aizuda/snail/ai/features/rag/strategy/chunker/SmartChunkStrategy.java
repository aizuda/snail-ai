package com.aizuda.snail.ai.features.rag.strategy.chunker;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.builder.ChatClientBuilder;
import com.aizuda.snail.ai.features.rag.enums.ChunkModeEnum;
import com.aizuda.snail.ai.model.enums.ModelTypeEnum;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能切片：LLM 输出语义片段作为一级切分，再经 {@link TokenAwareChunker#chunkParagraphs} 二级递归。
 */
@Slf4j
@Component
public class SmartChunkStrategy extends AbstractChunkStrategy {

    // 每段最大字符数，约等于 4k tokens，保证 LLM 响应稳定
    private static final int SEGMENT_CHARS = 10_000;
    // 单次 LLM 调用的最大字符数（小文档直接处理）
    private static final int MAX_LLM_INPUT_CHARS = 48_000;
    // 智能切片输出最大 tokens，确保足够输出完整的 JSON 数组
    private static final int CHUNK_MAX_TOKENS = 16_384;

    private static final String SYSTEM = """
            你是文档切片助手。请将用户提供的文本按语义切分为完整片段。
            
            要求：
            1. 输出JSON数组，每个元素是一个完整的语义片段
            2. 片段应完整、可独立理解
            3. 不要添加任何解释或Markdown格式
            4. 确保输出完整的JSON数组，以 ] 结尾
            
            输出格式：
            ["片段1","片段2","片段3"]
            """;

    private final ModelConfigHandler modelConfigHandler;
    private final ChatClientBuilder chatClientBuilder;

    public SmartChunkStrategy(
            TokenAwareChunker chunker,
            ModelConfigHandler modelConfigHandler,
            ChatClientBuilder chatClientBuilder) {
        super(chunker);
        this.modelConfigHandler = modelConfigHandler;
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public boolean supports(ChunkModeEnum mode) {
        return ChunkModeEnum.SMART == mode;
    }

    @Override
    protected String[] splitIntoParagraphs(ChunkContext ctx) {
        Long modelId = ctx.getChunkModelId();
        if (modelId == null) {
            log.warn("smart mode but chunkModelId null, fallback to length");
            return new String[]{ctx.getContent()};
        }

        ModelConfigInfoDTO config = modelConfigHandler.getConfigInfo(modelId);
        if (config == null) {
            throw new IllegalArgumentException("切片模型不存在: " + modelId);
        }
        if (!ModelTypeEnum.CHAT.getValue().equalsIgnoreCase(StrUtil.blankToDefault(config.getModelType(), ""))) {
            throw new IllegalArgumentException("智能切片须选择「对话」类模型");
        }
        String apiKey = modelConfigHandler.decryptApiKey(config.getEncryptedApiKey());
        if (StrUtil.isBlank(apiKey)) {
            throw new IllegalArgumentException("模型 API Key 不可用");
        }

        String content = ctx.getContent();
        ChatClient chatClient = chatClientBuilder.getOrBuildChatClient(apiKey, config);

        // 小文档直接处理
        if (content.length() <= MAX_LLM_INPUT_CHARS) {
            return doSmartChunk(chatClient, content);
        }

        // 大文档分段处理
        log.info("大文档智能切片：文档长度 {}，采用分段处理", content.length());
        return doSegmentedSmartChunk(chatClient, content);
    }

    /**
     * 单次智能切片
     */
    private String[] doSmartChunk(ChatClient chatClient, String content) {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM.trim()),
                new UserMessage("全文如下：\n\n" + content)
        ), buildChunkOptions());

        ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
        String raw = response.getResult().getOutput().getText();
        
        if (StrUtil.isBlank(raw)) {
            log.warn("智能切片模型返回空，fallback to length mode");
            return new String[]{content};
        }
        
        List<String> segments = parseJsonStringArray(raw);
        if (segments.isEmpty()) {
            log.warn("智能切片未解析到 JSON 数组，fallback");
            return new String[]{content};
        }
        return segments.toArray(new String[0]);
    }

    /**
     * 大文档分段智能切片
     */
    private String[] doSegmentedSmartChunk(ChatClient chatClient, String content) {
        List<String> allSegments = new ArrayList<>();
        int totalLen = content.length();
        int segmentCount = (int) Math.ceil((double) totalLen / SEGMENT_CHARS);

        for (int i = 0; i < segmentCount; i++) {
            int start = i * SEGMENT_CHARS;
            int end = Math.min(start + SEGMENT_CHARS, totalLen);
            String segment = content.substring(start, end);

            log.info("智能切片分段 {}/{}：字符范围 {}-{}", i + 1, segmentCount, start, end);

            List<String> segments = callLLMForChunk(chatClient, segment, i + 1, segmentCount);
            if (segments.isEmpty()) {
                allSegments.add(segment);
            } else {
                allSegments.addAll(segments);
            }
        }

        return allSegments.toArray(new String[0]);
    }

    /**
     * 调用 LLM 进行切片，带异常处理
     */
    private List<String> callLLMForChunk(ChatClient chatClient, String content, int segIndex, int totalSeg) {
        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(SYSTEM.trim()),
                    new UserMessage("文档第" + segIndex + "/" + totalSeg + "部分：\n\n" + content)
            ), buildChunkOptions());

            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            String raw = response.getResult().getOutput().getText();
            
            if (StrUtil.isBlank(raw)) {
                log.warn("分段 {} 模型返回空，保留原文", segIndex);
                return List.of();
            }

            List<String> segments = parseJsonStringArray(raw);
            if (segments.isEmpty()) {
                log.warn("分段 {} 未解析到 JSON 数组，保留原文", segIndex);
                return List.of();
            }
            return segments;
        } catch (Exception e) {
            log.warn("分段 {} 调用模型异常: {}，保留原文", segIndex, e.getMessage());
            return List.of();
        }
    }

    /**
     * 构建智能切片专用的 ChatOptions，设置足够大的 maxTokens
     */
    private OpenAiChatOptions buildChunkOptions() {
        return OpenAiChatOptions.builder()
                .maxTokens(CHUNK_MAX_TOKENS)
                .build();
    }

    private List<String> parseJsonStringArray(String raw) {
        String s = raw.trim();
        
        // 移除 Markdown 代码围栏
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            int endFence = s.lastIndexOf("```");
            if (firstNl > 0 && endFence > firstNl) {
                s = s.substring(firstNl + 1, endFence).trim();
            }
        }
        // 提取 JSON 数组部分
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start >= 0 && end > start) {
            s = s.substring(start, end + 1);
        }

        // 尝试标准 JSON 解析
        try {
            var jsonNode = JsonUtil.toJson(s);
            if (jsonNode != null && jsonNode.isArray()) {
                List<String> result = new ArrayList<>();
                for (var node : jsonNode) {
                    String text = node.asText();
                    if (StrUtil.isNotBlank(text)) {
                        result.add(text.trim());
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (Exception e) {
            log.debug("标准 JSON 解析失败，尝试正则提取: {}", e.getMessage());
        }

        // Fallback：使用正则提取 JSON 数组中的字符串
        return extractStringsFromJson(s);
    }

    /**
     * 从 JSON 数组字符串中提取内容（处理 LLM 返回的不规范 JSON）
     */
    private List<String> extractStringsFromJson(String json) {
        List<String> result = new ArrayList<>();
        
        // 移除外层的 [ ]
        String content = json;
        if (content.startsWith("[")) {
            content = content.substring(1);
        }
        if (content.endsWith("]")) {
            content = content.substring(0, content.length() - 1);
        }
        
        // 匹配 "..." 格式的字符串，处理转义引号
        // 使用更健壮的模式：匹配从 " 开始到下一个非转义的 " 结束
        Pattern pattern = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            try {
                String text = matcher.group(1);
                // 处理常见的 JSON 转义
                text = text
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
                
                if (StrUtil.isNotBlank(text) && text.length() > 20) {
                    result.add(text.trim());
                }
            } catch (Exception e) {
                // 忽略单个解析错误
            }
        }
        
        if (!result.isEmpty()) {
            log.info("正则提取了 {} 个片段", result.size());
        }
        
        return result;
    }
}
