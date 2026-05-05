package com.aizuda.snail.ai.features.rag.strategy.chunker;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.execption.SnailAiCommonException;
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
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 智能切片：LLM 输出语义片段作为一级切分，再经 {@link TokenAwareChunker#chunkParagraphs} 二级递归。
 */
@Slf4j
@Component
public class SmartChunkStrategy extends AbstractChunkStrategy {

    private static final int MAX_LLM_INPUT_CHARS = 48_000;

    private static final String SYSTEM = """
            你是文档切片助手。用户会提供一段需要入库做向量检索的纯文本。
            请按语义将文本切分为多个片段，每个片段尽量完整、可独立检索。
            只输出一个 JSON 数组，元素为字符串，不要 Markdown、不要代码围栏、不要解释。
            若文本极长，可切分为至多 80 段。
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

        String toSend = ctx.getContent().length() > MAX_LLM_INPUT_CHARS
                ? ctx.getContent().substring(0, MAX_LLM_INPUT_CHARS)
                : ctx.getContent();

        ChatClient chatClient = chatClientBuilder.getOrBuildChatClient(apiKey, config);
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM.trim()),
                new UserMessage("全文如下：\n\n" + toSend)
        ));

        String raw = chatClient.prompt(prompt).call().content();
        if (StrUtil.isBlank(raw)) {
            throw new SnailAiCommonException("智能切片模型返回空，fallback");
        }
        List<String> segments = parseJsonStringArray(raw);
        if (segments.isEmpty()) {
            log.warn("智能切片未解析到 JSON 数组，fallback");
            return new String[]{ctx.getContent()};
        }
        return segments.toArray(new String[0]);
    }

    private List<String> parseJsonStringArray(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            int endFence = s.lastIndexOf("```");
            if (firstNl > 0 && endFence > firstNl) {
                s = s.substring(firstNl + 1, endFence).trim();
            }
        }
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start >= 0 && end > start) {
            s = s.substring(start, end + 1);
        }
        try {
            List<String> list = JsonUtil.parseList(s, String.class);
            return list != null ? list.stream().filter(StrUtil::isNotBlank).map(String::trim).toList() : List.of();
        } catch (Exception e) {
            log.warn("解析智能切片 JSON 失败: {}", e.getMessage());
            return List.of();
        }
    }
}
