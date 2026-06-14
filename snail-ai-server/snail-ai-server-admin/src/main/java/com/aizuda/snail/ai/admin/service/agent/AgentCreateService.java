package com.aizuda.snail.ai.admin.service.agent;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.admin.dto.AgentCreateStreamEvent;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.builder.chat.ChatClientBuilder;
import com.aizuda.snail.ai.admin.service.model.AiModelConfigService;
import com.aizuda.snail.ai.model.enums.ModelTypeEnum;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentMapper;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigVO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCreateService {

    private final AgentMapper agentMapper;
    private final AiModelConfigService aiModelConfigService;
    private final ModelConfigHandler modelConfigHandler;
    private final ChatClientBuilder chatClientBuilder;

    /**
     * Agent信息记录
     */
    private record AgentInfo(String name, String description, String greeting, List<String> presetQuestions, String instruction) {}

    /**
     * 流式创建智能体（单次LLM调用 + 阶段性推送）
     */
    public Flux<AgentCreateStreamEvent> createByDescriptionStream(String description) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        return Flux.create(sink -> {
            Thread.startVirtualThread(() -> doCreateStream(description, userId, sink));
            sink.onCancel(() -> log.debug("Agent create stream cancelled"));
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    private void doCreateStream(String description, Long userId, FluxSink<AgentCreateStreamEvent> sink) {
        try {
            // 1. 发送开始信号
            emit(sink, AgentCreateStreamEvent.start());

            // 2. 获取默认 CHAT 模型
            AiModelConfigVO defaultModel = aiModelConfigService.getDefaultModelByType(ModelTypeEnum.CHAT.getValue());
            if (defaultModel == null) {
                emitError(sink, "未找到默认CHAT模型");
                return;
            }

            // 3. 构建 ChatClient
            ChatClient chatClient;
            try {
                ModelConfigInfoDTO configInfo = modelConfigHandler.getConfigInfo(defaultModel.getId());
                String apiKey = modelConfigHandler.decryptApiKey(configInfo.getEncryptedApiKey());
                chatClient = chatClientBuilder.getOrBuildChatClient(apiKey, configInfo);
            } catch (Exception e) {
                log.error("构建 ChatClient 失败", e);
                emitError(sink, "构建ChatClient失败");
                return;
            }

            // 4. 构建结构化Prompt
            String structuredPrompt = buildStructuredPrompt(description);

            // 5. 累积缓冲区和字段推送标记
            StringBuilder buffer = new StringBuilder();
            Set<String> pushedFields = ConcurrentHashMap.newKeySet();

            // 6. 流式调用LLM
            chatClient.prompt()
                    .user(structuredPrompt)
                    .stream()
                    .chatResponse()
                    .subscribe(
                            chatResponse -> {
                                String text = java.util.Optional.ofNullable(chatResponse)
                                        .map(org.springframework.ai.chat.model.ChatResponse::getResult)
                                        .map(org.springframework.ai.chat.model.Generation::getOutput)
                                        .map(org.springframework.ai.chat.messages.AssistantMessage::getText)
                                        .orElse(null);

                                if (StringUtils.hasText(text)) {
                                    buffer.append(text);
                                    tryDetectAndPushFields(buffer.toString(), pushedFields, sink);
                                }
                            },
                            error -> {
                                log.error("流式创建失败", error);
                                emitError(sink, error.getMessage());
                            },
                            () -> {
                                try {
                                    String jsonContent = extractJson(buffer.toString());
                                    AgentInfo agentInfo = parseAgentInfo(jsonContent);
                                    ensureAllFieldsPushed(agentInfo, pushedFields, sink);
                                    emit(sink, AgentCreateStreamEvent.done(null));
                                    complete(sink);
                                } catch (Exception e) {
                                    log.error("生成内容失败", e);
                                    emitError(sink, e.getMessage());
                                }
                            }
                    );

        } catch (Exception e) {
            log.error("初始化流式创建失败", e);
            emitError(sink, e.getMessage());
        }
    }

    private String buildStructuredPrompt(String userDescription) {
        return String.format("""
                你是一个AI助手设计专家。请根据用户的需求描述，设计一个智能助手。
                
                用户需求：%s
                
                请严格按照以下JSON格式输出，不要包含任何其他文字说明：
                
                {
                  "name": "智能助手的名称（简短有吸引力，5-10个中文字）",
                  "description": "详细描述（100-200字，说明功能、适用场景、特点）",
                  "greeting": "欢迎语（亲切友好，30-50字，简洁的自我介绍）",
                  "presetQuestions": [
                    "推荐问题1（具体实用的示例问题）",
                    "推荐问题2",
                    "推荐问题3"
                  ],
                  "instruction": "系统指令（详细的角色设定和行为指南，300-500字，包含：角色定位、专业领域、回答风格、注意事项等）"
                }
                
                要求：
                1. 所有内容必须是中文
                2. 名称要有创意且易记
                3. 描述要突出核心价值
                4. 欢迎语仅为简洁的问候和自我介绍，不包含问题列表
                5. presetQuestions为3-5个推荐问题，用于引导用户开始对话
                6. 指令要详细具体，能有效引导AI行为
                7. 严格遵守JSON格式，确保可解析
                
                现在请直接输出JSON：
                """, userDescription);
    }

    private AgentInfo parseAgentInfo(String jsonResponse) {
        try {
            String cleanJson = jsonResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            int startIndex = cleanJson.indexOf('{');
            int endIndex = cleanJson.lastIndexOf('}');

            if (startIndex == -1 || endIndex == -1) {
                throw new SnailAiException("响应中未找到有效的JSON对象");
            }

            String jsonContent = cleanJson.substring(startIndex, endIndex + 1);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonContent);

            List<String> presetQuestions = new ArrayList<>();
            JsonNode questionsNode = root.get("presetQuestions");
            if (questionsNode != null && questionsNode.isArray()) {
                for (JsonNode questionNode : questionsNode) {
                    String question = questionNode.asText().trim();
                    if (StrUtil.isNotBlank(question)) {
                        presetQuestions.add(question);
                    }
                }
            }

            return new AgentInfo(
                    root.get("name").asText(),
                    root.get("description").asText(),
                    root.get("greeting").asText(),
                    presetQuestions,
                    root.get("instruction").asText()
            );
        } catch (Exception e) {
            log.error("解析JSON失败，原始响应：{}", jsonResponse, e);
            throw new SnailAiException("解析智能体信息失败：" + e.getMessage(), e);
        }
    }

    private void tryDetectAndPushFields(String content, Set<String> pushedFields, FluxSink<AgentCreateStreamEvent> sink) {
        try {
            String[] stringFields = {"name", "description", "greeting", "instruction"};

            for (String field : stringFields) {
                if (pushedFields.contains(field)) {
                    continue;
                }

                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "\"" + field + "\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"\\s*[,}]",
                        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.MULTILINE
                );

                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    String value = matcher.group(1);
                    emit(sink, AgentCreateStreamEvent.fieldDone(field, value));
                    pushedFields.add(field);
                    log.info("字段 {} 完成并推送", field);
                }
            }

            if (!pushedFields.contains("presetQuestions")) {
                java.util.regex.Pattern arrayPattern = java.util.regex.Pattern.compile(
                        "\"presetQuestions\"\\s*:\\s*\\[([^\\]]*)]",
                        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.MULTILINE
                );

                java.util.regex.Matcher arrayMatcher = arrayPattern.matcher(content);
                if (arrayMatcher.find()) {
                    String arrayJson = "[" + arrayMatcher.group(1).replaceAll("\\s*\\n\\s*", " ").trim() + "]";
                    emit(sink, AgentCreateStreamEvent.fieldDone("presetQuestions", arrayJson));
                    pushedFields.add("presetQuestions");
                    log.info("字段 presetQuestions 完成并推送");
                }
            }
        } catch (Exception e) {
            log.warn("检测字段失败", e);
        }
    }

    private String extractJson(String content) {
        String clean = content
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        int start = clean.indexOf('{');
        int end = clean.lastIndexOf('}');

        if (start == -1 || end == -1) {
            throw new SnailAiException("未找到有效JSON");
        }

        return clean.substring(start, end + 1);
    }

    private void ensureAllFieldsPushed(AgentInfo agentInfo, Set<String> pushedFields, FluxSink<AgentCreateStreamEvent> sink) {
        if (!pushedFields.contains("name")) {
            emit(sink, AgentCreateStreamEvent.fieldDone(AgentCreateStreamEvent.FIELD_NAME, agentInfo.name()));
        }
        if (!pushedFields.contains("description")) {
            emit(sink, AgentCreateStreamEvent.fieldDone(AgentCreateStreamEvent.FIELD_DESCRIPTION, agentInfo.description()));
        }
        if (!pushedFields.contains("greeting")) {
            emit(sink, AgentCreateStreamEvent.fieldDone(AgentCreateStreamEvent.FIELD_GREETING, agentInfo.greeting()));
        }
        if (!pushedFields.contains("presetQuestions") && !agentInfo.presetQuestions().isEmpty()) {
            String questionsJson = JsonUtil.toJsonString(agentInfo.presetQuestions());
            emit(sink, AgentCreateStreamEvent.fieldDone(AgentCreateStreamEvent.FIELD_PRESET_QUESTIONS, questionsJson));
        }
        if (!pushedFields.contains("instruction")) {
            emit(sink, AgentCreateStreamEvent.fieldDone(AgentCreateStreamEvent.FIELD_INSTRUCTION, agentInfo.instruction()));
        }
    }

    private void emit(FluxSink<AgentCreateStreamEvent> sink, AgentCreateStreamEvent event) {
        if (!sink.isCancelled()) {
            sink.next(event);
        }
    }

    private void complete(FluxSink<AgentCreateStreamEvent> sink) {
        if (!sink.isCancelled()) {
            sink.complete();
        }
    }

    private void emitError(FluxSink<AgentCreateStreamEvent> sink, String message) {
        if (!sink.isCancelled()) {
            sink.next(AgentCreateStreamEvent.error(message != null ? message : "Unknown error"));
            sink.complete();
        }
    }
}
