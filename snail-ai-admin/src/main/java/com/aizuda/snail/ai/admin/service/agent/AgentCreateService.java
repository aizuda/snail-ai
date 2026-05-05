package com.aizuda.snail.ai.admin.service.agent;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snail.ai.common.enums.agent.AgentStatusEnum;
import com.aizuda.snail.ai.common.execption.SnailAiException;
import com.aizuda.snail.ai.common.util.JsonUtil;
import com.aizuda.snail.ai.model.builder.ChatClientBuilder;
import com.aizuda.snail.ai.admin.service.model.AiModelConfigService;
import com.aizuda.snail.ai.model.enums.ModelTypeEnum;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.persistence.agent.mapper.AgentMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.aizuda.snail.ai.admin.vo.model.AiModelConfigVO;
import com.aizuda.snail.ai.persistence.security.UserSessionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCreateService {

    private final AgentMapper agentMapper;
    private final AiModelConfigService aiModelConfigService;
    private final ModelConfigHandler modelConfigHandler;
    private final ChatClientBuilder chatClientBuilder;
    private final Executor snailAiAsyncExecutor;

    /**
     * Agent信息记录
     */
    private record AgentInfo(String name, String description, String greeting, List<String> presetQuestions, String instruction) {}

    /**
     * 流式创建智能体（单次LLM调用 + 阶段性推送）
     */
    public void createByDescriptionStream(String description, ResponseBodyEmitter emitter) {
        Long userId = UserSessionUtils.currentUserSession().getId();
        CompletableFuture.runAsync(() -> doCreateStream(description, userId, emitter), snailAiAsyncExecutor);
    }

    private void doCreateStream(String description, Long userId, ResponseBodyEmitter emitter) {
        try {
            // 1. 发送开始信号
            emitter.send("[START]\n");

            // 2. 获取默认 CHAT 模型
            AiModelConfigVO defaultModel = aiModelConfigService.getDefaultModelByType(ModelTypeEnum.CHAT.getValue());
            if (defaultModel == null) {
                emitter.send("[ERROR]未找到默认CHAT模型\n");
                emitter.completeWithError(new SnailAiException("未找到默认CHAT模型"));
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
                emitter.send("[ERROR]构建ChatClient失败\n");
                emitter.completeWithError(e);
                return;
            }

            // 4. 构建结构化Prompt
            String structuredPrompt = buildStructuredPrompt(description);

            // 5. 累积缓冲区和字段推送标记
            AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());
            Set<String> pushedFields = java.util.concurrent.ConcurrentHashMap.newKeySet();

            // 6. 流式调用LLM
            chatClient.prompt()
                    .user(structuredPrompt)
                    .stream()
                    .chatResponse()
                    .subscribe(
                            chatResponse -> {
                                // onNext: 处理每个chunk
                                String text = java.util.Optional.ofNullable(chatResponse)
                                        .map(org.springframework.ai.chat.model.ChatResponse::getResult)
                                        .map(org.springframework.ai.chat.model.Generation::getOutput)
                                        .map(org.springframework.ai.chat.messages.AssistantMessage::getText)
                                        .orElse(null);

                                if (org.springframework.util.StringUtils.hasText(text)) {
                                    buffer.get().append(text);

                                    // 尝试检测字段完成
                                    tryDetectAndPushFields(buffer.get().toString(), pushedFields, emitter);
                                }
                            },
                            error -> {
                                // onError: 错误处理
                                log.error("流式创建失败", error);
                                try {
                                    emitter.send("[ERROR]" + error.getMessage() + "\n");
                                    emitter.completeWithError(error);
                                } catch (IOException ignored) {
                                }
                            },
                            () -> {
                                // onComplete: 流结束，保存到数据库
                                try {
                                    String jsonContent = extractJson(buffer.get().toString());
                                    AgentInfo agentInfo = parseAgentInfo(jsonContent);

                                    // 确保所有字段都推送了（防止漏推）
                                    ensureAllFieldsPushed(agentInfo, pushedFields, emitter);

                                    // 保存到数据库
                                    AgentPO agent = AgentPO.builder()
                                            .name(agentInfo.name())
                                            .description(agentInfo.description())
                                            .instruction(agentInfo.instruction())
                                            .greeting(agentInfo.greeting())
                                            .presetQuestions(agentInfo.presetQuestions().isEmpty() ? null : JsonUtil.toJsonString(agentInfo.presetQuestions()))
                                            .avatar(null)
                                            .chatModelId(defaultModel.getId())
                                            .creatorId(userId)
                                            .ragId(0L)
                                            .status(AgentStatusEnum.ACTIVE.getStatus())
                                            .viewCount(0)
                                            .mcpEnabled(false)
                                            .webSearchEnabled(false)
                                            .ragEnabled(false)
                                            .isFeatured(false)
                                            .build();
                                    agentMapper.insert(agent);

                                    emitter.send("[DONE]" + agent.getId() + "\n");
                                    emitter.complete();
                                } catch (Exception e) {
                                    log.error("保存智能体失败", e);
                                    try {
                                        emitter.send("[ERROR]保存失败\n");
                                        emitter.completeWithError(e);
                                    } catch (IOException ignored) {
                                    }
                                }
                            }
                    );

        } catch (Exception e) {
            log.error("初始化流式创建失败", e);
            try {
                emitter.send("[ERROR]" + e.getMessage() + "\n");
                emitter.completeWithError(e);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 构建结构化Prompt
     */
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

    /**
     * 解析JSON响应
     */
    private AgentInfo parseAgentInfo(String jsonResponse) {
        try {
            // 1. 清理可能的Markdown代码块标记
            String cleanJson = jsonResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            // 2. 查找JSON对象的起止位置
            int startIndex = cleanJson.indexOf('{');
            int endIndex = cleanJson.lastIndexOf('}');

            if (startIndex == -1 || endIndex == -1) {
                throw new SnailAiException("响应中未找到有效的JSON对象");
            }

            String jsonContent = cleanJson.substring(startIndex, endIndex + 1);

            // 3. 使用Jackson解析
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonContent);

            // 解析预设问题数组
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

    /**
     * 尝试检测并推送已完成的字段
     */
    private void tryDetectAndPushFields(String content, Set<String> pushedFields, ResponseBodyEmitter emitter) {
        try {
            // 字段顺序：name -> description -> greeting -> presetQuestions -> instruction
            String[] stringFields = {"name", "description", "greeting", "instruction"};

            // 检测字符串类型字段
            for (String field : stringFields) {
                if (pushedFields.contains(field)) {
                    continue; // 已推送过，跳过
                }

                // 使用正则检测完整字段（含引号和逗号/闭合括号）
                // 匹配："fieldName": "value",  或 "fieldName": "value"}
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "\"" + field + "\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"\\s*[,}]",
                        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.MULTILINE
                );

                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    String value = matcher.group(1);

                    // 字段完成，立即推送
                    emitter.send("[FIELD_DONE]" + field + ":" + value + "\n");
                    pushedFields.add(field);

                    log.info("字段 {} 完成并推送", field);
                }
            }

            // 检测 presetQuestions 数组字段
            if (!pushedFields.contains("presetQuestions")) {
                // 匹配 "presetQuestions": [...] 格式（数组可能跨多行）
                java.util.regex.Pattern arrayPattern = java.util.regex.Pattern.compile(
                        "\"presetQuestions\"\\s*:\\s*\\[([^\\]]*)]",
                        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.MULTILINE
                );

                java.util.regex.Matcher arrayMatcher = arrayPattern.matcher(content);
                if (arrayMatcher.find()) {
                    String arrayContent = arrayMatcher.group(0); // 包含完整的 "presetQuestions": [...]
                    
                    // 提取数组部分 [...]
                    int bracketStart = arrayContent.indexOf('[');
                    if (bracketStart != -1) {
                        String arrayJson = arrayContent.substring(bracketStart);
                        
                        // 字段完成，推送完整 JSON 数组
                        emitter.send("[FIELD_DONE]presetQuestions:" + arrayJson + "\n");
                        pushedFields.add("presetQuestions");

                        log.info("字段 presetQuestions 完成并推送");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("检测字段失败", e);
        }
    }

    /**
     * 提取纯JSON内容（去除Markdown包裹）
     */
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

    /**
     * 确保所有字段都已推送（防止漏推）
     */
    private void ensureAllFieldsPushed(AgentInfo agentInfo, Set<String> pushedFields, ResponseBodyEmitter emitter) {
        try {
            if (!pushedFields.contains("name")) {
                emitter.send("[FIELD_DONE]name:" + agentInfo.name() + "\n");
            }
            if (!pushedFields.contains("description")) {
                emitter.send("[FIELD_DONE]description:" + agentInfo.description() + "\n");
            }
            if (!pushedFields.contains("greeting")) {
                emitter.send("[FIELD_DONE]greeting:" + agentInfo.greeting() + "\n");
            }
            if (!pushedFields.contains("presetQuestions") && !agentInfo.presetQuestions().isEmpty()) {
                String questionsJson = JsonUtil.toJsonString(agentInfo.presetQuestions());
                emitter.send("[FIELD_DONE]presetQuestions:" + questionsJson + "\n");
            }
            if (!pushedFields.contains("instruction")) {
                emitter.send("[FIELD_DONE]instruction:" + agentInfo.instruction() + "\n");
            }
        } catch (IOException e) {
            log.warn("补推字段失败", e);
        }
    }
}
