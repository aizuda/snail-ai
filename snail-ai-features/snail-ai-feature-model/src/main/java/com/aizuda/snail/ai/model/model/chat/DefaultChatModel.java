package com.aizuda.snail.ai.model.model.chat;

import com.aizuda.snail.ai.common.log.SnailAiLog;
import com.aizuda.snail.ai.common.model.ModelCallException;
import com.aizuda.snail.ai.model.builder.ChatClientBuilder;
import com.aizuda.snail.ai.model.handle.ModelConfigHandler;
import com.aizuda.snail.ai.model.dto.ModelConfigInfoDTO;
import com.aizuda.snail.ai.model.model.AbstractModel;
import com.aizuda.snail.ai.model.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * author: opensnail
 * date: 2026-03-04
 */
@Slf4j
@Service()
@RequiredArgsConstructor
@Scope("prototype")
public class DefaultChatModel extends AbstractModel implements ChatModel {
    private final ChatClientBuilder chatClientBuilder;
    private final McpToolService mcpToolService;
    private final ModelConfigHandler modelConfigHandler;

    @Override
    public boolean supports(String modelKey) {
        return true;
    }

    @Override
    public String chatModel(ChatModelDTO chatModelDTO) throws ModelCallException {
        return callModel(chatModelDTO.userContext(), chatModelDTO.systemContext());
    }

    @Override
    public void chatStreamModel(ChatStreamModelDTO chatModelDTO) throws ModelCallException {
        callModelStream(chatModelDTO.userContext(), chatModelDTO.systemContext(),
                chatModelDTO.messageConsumer(), chatModelDTO.onComplete(), chatModelDTO.onError());
    }

    /**
     * 基于配置ID的流式调用
     * 适用于需要实时显示输出的场景（如聊天对话）
     *
     * @param userContext     用户输入内容
     * @param systemContext   系统提示词
     * @param messageConsumer 消息消费回调，每条返回的消息都会调用此方法
     * @throws ModelCallException 如果配置不存在、无权限、调用失败等
     */
    public void callModelStream(String userContext,
                                String systemContext,
                                Consumer<String> messageConsumer,
                                Runnable onComplete,
                                Consumer<Throwable> onError)
            throws ModelCallException {
        Long modelConfigId = modelConfigInfo.getId();

        // 1. 参数验证
        validateInputs(modelConfigInfo.getId(), userContext);

        long startTime = System.currentTimeMillis();

        try {
            // 3. 解密 API Key
            String decryptedApiKey = decryptApiKey(modelConfigInfo);

            // 4. 获取或构建 ChatClient
            ChatClient chatClient = chatClientBuilder.getOrBuildChatClient(
                    decryptedApiKey,
                    modelConfigInfo
            );

            // 5. 构建消息
            Prompt prompt = buildPrompt(userContext, systemContext);

            // 6. 流式调用
            SnailAiLog.LOCAL.info("Stream calling model: {}, config: {}",
                    modelConfigInfo.getModelName(), modelConfigId);

            // 构建 prompt request，注入 MCP 工具和 Skill 工具（如果有）
            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt(prompt);

            log.debug("prompt:{}", prompt.getSystemMessage());
            requestSpec
                    .stream()
                    .chatResponse()
                    .subscribe(
                            chatResponse -> {
                                // 提取文本内容
                                String text = java.util.Optional.ofNullable(chatResponse)
                                        .map(org.springframework.ai.chat.model.ChatResponse::getResult)
                                        .map(org.springframework.ai.chat.model.Generation::getOutput)
                                        .map(org.springframework.ai.chat.messages.AbstractMessage::getText)
                                        .orElse(null);
                                if (StringUtils.hasText(text)) {
                                    messageConsumer.accept(text);
                                }
                            },
                            error -> {
                                long duration = System.currentTimeMillis() - startTime;
                                SnailAiLog.LOCAL.error("Stream call failed for model: {}, error: {}",
                                        modelConfigId, error.getMessage(), error);
                                if (onError != null) {
                                    onError.accept(error);
                                }
                            },
                            () -> {
                                // 调用完成
                                long duration = System.currentTimeMillis() - startTime;
                                SnailAiLog.LOCAL.info("Stream call completed for model: {}, duration: {}ms",
                                        modelConfigId, duration);
                                if (onComplete != null) {
                                    onComplete.run();
                                }
                            }
                    );

        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            SnailAiLog.LOCAL.error("Stream call failed for config: {}, error: {}",
                    modelConfigId, e.getMessage(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.MODEL_CALL_FAILED,
                    "模型调用失败: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 基于配置ID的非流式调用
     * 适用于需要完整结果的场景（如翻译、总结等）
     *
     * @param userContext   用户输入内容
     * @param systemContext 系统提示词
     * @return 模型返回的完整回复
     * @throws ModelCallException 如果配置不存在、无权限、调用失败等
     */
    public String callModel(String userContext,
                            String systemContext)
            throws ModelCallException {

        // 1. 参数验证
//        validateInputs(userContext);

        long startTime = System.currentTimeMillis();

        try {
            // 3. 解密 API Key
            String decryptedApiKey = decryptApiKey(modelConfigInfo);

            // 4. 获取或构建 ChatClient
            ChatClient chatClient = chatClientBuilder.getOrBuildChatClient(
                    decryptedApiKey,
                    modelConfigInfo
            );

            // 5. 构建消息
            Prompt prompt = buildPrompt(userContext, systemContext);

            // 6. 非流式调用
            SnailAiLog.LOCAL.info("Calling model: {}, config: {}",
                    modelConfigInfo.getModelName(), modelConfigInfo.getId());

            String result = chatClient.prompt(prompt)
                    .call()
                    .content();

            // 7. 记录完成日志
            long duration = System.currentTimeMillis() - startTime;

            SnailAiLog.LOCAL.info("Model call completed: {}, duration: {}ms",
                    modelConfigInfo.getId(), duration);

            return result;

        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            SnailAiLog.LOCAL.error("Model call failed for config: {}, error: {}",
                    modelConfigInfo.getId(), e.getMessage(), e);
            throw new ModelCallException(
                    ModelCallException.ErrorCode.MODEL_CALL_FAILED,
                    "模型调用失败: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 参数验证
     */
    private void validateInputs(Long modelConfigId, String userContext) throws ModelCallException {
        if (modelConfigId == null || modelConfigId <= 0) {
            throw new ModelCallException(ModelCallException.ErrorCode.INVALID_PARAMETER,
                    "模型配置ID不能为空且必须大于0");
        }
        if (!StringUtils.hasText(userContext)) {
            throw new ModelCallException(ModelCallException.ErrorCode.INVALID_PARAMETER,
                    "用户输入内容不能为空");
        }
    }


    /**
     * 解密 API Key
     */
    private String decryptApiKey(ModelConfigInfoDTO config) throws ModelCallException {
        try {
            // 调用服务层获取解密的API Key
            String decryptedKey = modelConfigHandler.decryptApiKey(config.getEncryptedApiKey());
            if (!StringUtils.hasText(decryptedKey)) {
                throw new ModelCallException(ModelCallException.ErrorCode.API_KEY_DECRYPT_FAILED,
                        "API Key解密失败或不存在");
            }
            return decryptedKey;
        } catch (ModelCallException e) {
            throw e;
        } catch (Exception e) {
            SnailAiLog.LOCAL.error("Failed to decrypt API Key for config: {}", config.getId(), e);
            throw new ModelCallException(ModelCallException.ErrorCode.API_KEY_DECRYPT_FAILED,
                    "API Key解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建 Prompt
     */
    private Prompt buildPrompt(String userContext, String systemContext) {
        // 构建消息列表
        List<Message> messages = new ArrayList<>();

        // 添加系统提示词
        if (StringUtils.hasText(systemContext)) {
            messages.add(new SystemMessage(systemContext));
        }

        // 添加用户输入
        messages.add(new UserMessage(userContext));

        return new Prompt(messages);
    }


}
