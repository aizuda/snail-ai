package com.aizuda.snail.ai.features.rag.strategy.chunker;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.builder.chat.ChatClientBuilder;
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
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能切片：LLM 输出语义片段作为一级切分，再经 {@link TokenAwareChunker#chunkParagraphs} 二级递归。
 */
@Slf4j
@Component
public class SmartChunkStrategy extends AbstractChunkStrategy {

    // 每段最大字符数，降低单次 LLM 输入长度以加快推理
    private static final int SEGMENT_CHARS = 8_000;
    // 单次 LLM 调用的最大字符数（超过此值走分段处理）
    private static final int MAX_LLM_INPUT_CHARS = 16_000;
    // 智能切片输出最大 tokens，缩小输出范围降低超时风险
    private static final int CHUNK_MAX_TOKENS = 4_096;
    // LLM 调用最大重试次数
    private static final int MAX_RETRIES = 2;
    // 重试间隔基数（毫秒）
    private static final long RETRY_BASE_DELAY_MS = 1_000L;
    // 最大并发 LLM 调用数，防止批量文档处理打满模型接口
    private static final int MAX_CONCURRENT_LLM_CALLS = 3;
    // 获取信号量许可的超时（秒），防止死等
    private static final long SEMAPHORE_ACQUIRE_TIMEOUT_SEC = 120L;

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
    private final Semaphore llmSemaphore = new Semaphore(MAX_CONCURRENT_LLM_CALLS);

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
            return doSmartChunk(chatClient, content, config);
        }

        // 大文档分段处理
        log.info("大文档智能切片：文档长度 {}，采用分段处理", content.length());
        return doSegmentedSmartChunk(chatClient, content, config);
    }

    /**
     * 单次智能切片
     */
    private String[] doSmartChunk(ChatClient chatClient, String content, ModelConfigInfoDTO config) {
        try {
            UserMessage userMessage = new UserMessage("全文如下：\n\n" + content);
            String raw = callLLMWithRetry(chatClient, userMessage, config);
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
        } catch (Exception e) {
            log.warn("智能切片调用模型异常: {}，fallback to length mode", e.getMessage());
            return new String[]{content};
        }
    }

    /**
     * 大文档分段智能切片（并行）。
     */
    private String[] doSegmentedSmartChunk(ChatClient chatClient, String content, ModelConfigInfoDTO config) {
        int totalLen = content.length();
        int segmentCount = (int) Math.ceil((double) totalLen / SEGMENT_CHARS);

        @SuppressWarnings("unchecked")
        CompletableFuture<List<String>>[] futures = new CompletableFuture[segmentCount];

        for (int i = 0; i < segmentCount; i++) {
            int segIdx = i;
            int start = segIdx * SEGMENT_CHARS;
            int end = Math.min(start + SEGMENT_CHARS, totalLen);
            String segment = content.substring(start, end);

            futures[i] = CompletableFuture.supplyAsync(() -> {
                log.info("智能切片分段 {}/{}：字符范围 {}-{}", segIdx + 1, segmentCount, start, end);
                return callLLMForChunk(chatClient, segment, segIdx + 1, segmentCount, config);
            });
        }

        List<String> allSegments = new ArrayList<>();
        for (int i = 0; i < segmentCount; i++) {
            try {
                List<String> segments = futures[i].join();
                if (segments.isEmpty()) {
                    int s = i * SEGMENT_CHARS;
                    int e = Math.min(s + SEGMENT_CHARS, totalLen);
                    allSegments.add(content.substring(s, e));
                } else {
                    allSegments.addAll(segments);
                }
            } catch (Exception e) {
                log.warn("分段 {} 并行执行异常: {}，保留原文", i + 1, e.getMessage());
                int s = i * SEGMENT_CHARS;
                int f = Math.min(s + SEGMENT_CHARS, totalLen);
                allSegments.add(content.substring(s, f));
            }
        }

        return allSegments.toArray(new String[0]);
    }

    /**
     * 调用 LLM 进行切片，带异常处理和重试
     */
    private List<String> callLLMForChunk(ChatClient chatClient, String content, int segIndex, int totalSeg, ModelConfigInfoDTO config) {
        try {
            UserMessage userMessage = new UserMessage("文档第" + segIndex + "/" + totalSeg + "部分：\n\n" + content);
            String raw = callLLMWithRetry(chatClient, userMessage, config);

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
     * 带重试和并发限流的 LLM 调用。
     */
    private String callLLMWithRetry(ChatClient chatClient, UserMessage userMessage, ModelConfigInfoDTO config) {
        boolean acquired = false;
        try {
            acquired = llmSemaphore.tryAcquire(SEMAPHORE_ACQUIRE_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RuntimeException("LLM 调用限流：等待信号量超时（" + SEMAPHORE_ACQUIRE_TIMEOUT_SEC + "s）");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("LLM 调用被中断（等待信号量）", e);
        }

        try {
            Exception lastException = null;
            for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
                try {
                    Prompt prompt = new Prompt(List.of(
                            new SystemMessage(SYSTEM.trim()),
                            userMessage
                    ), buildChunkOptions());

                    ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
                    String raw = response.getResult().getOutput().getText();
                    if (StrUtil.isNotBlank(raw)) {
                        return raw;
                    }
                    lastException = new RuntimeException("模型返回空");
                } catch (Exception e) {
                    lastException = e;
                    if (attempt < MAX_RETRIES) {
                        long delay = RETRY_BASE_DELAY_MS * (attempt + 1);
                        log.warn("LLM 调用失败 (attempt {}/{}, endpoint={}, model={}): {}，{}ms 后重试",
                                attempt + 1, MAX_RETRIES + 1,
                                config.getApiEndpoint(), config.getModelKey(),
                                e.getMessage(), delay);
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("LLM 调用被中断", ie);
                        }
                    }
                }
            }
            throw new RuntimeException("LLM 调用重试耗尽（" + (MAX_RETRIES + 1) + " 次）", lastException);
        } finally {
            if (acquired) {
                llmSemaphore.release();
            }
        }
    }

    /**
     * 构建智能切片专用的 ChatOptions，设置足够大的 maxTokens
     */
    private ChatOptions buildChunkOptions() {
        return ChatOptions.builder()
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
