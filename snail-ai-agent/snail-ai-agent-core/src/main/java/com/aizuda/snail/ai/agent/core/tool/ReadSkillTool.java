package com.aizuda.snail.ai.agent.core.tool;

import com.aizuda.snail.ai.agent.common.rpc.RpcClient;
import com.aizuda.snail.ai.common.dto.agent.SkillContentResponse;
import com.aizuda.snail.ai.common.dto.agent.SkillContentRequest;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端版 read_skill 工具（与 Server 端 ReadSkillTool 对齐）
 * <p>
 * 设计：
 * <ol>
 *   <li>首次调用时同步获取 skillContent（通过 gRPC 回调 Server）并缓存</li>
 *   <li>支撑文件（scripts、references 等）异步写入本地临时目录，不阻塞返回</li>
 *   <li>后续同一 skill 的重复调用直接返回缓存，不再请求 Server</li>
 * </ol>
 */
@Slf4j
public class ReadSkillTool {

    private final Map<String, ChatDispatchRequest.SkillDescriptor> skillRegistry;
    private final RpcClient rpcClient;
    private final String tempDir;

    /** 缓存：skillId → skillContent（避免同一对话中重复回调 Server） */
    private final ConcurrentHashMap<Long, String> contentCache = new ConcurrentHashMap<>();

    public ReadSkillTool(List<ChatDispatchRequest.SkillDescriptor> skills,
                         RpcClient rpcClient,
                         String tempDir) {
        this.rpcClient = rpcClient;
        this.tempDir = tempDir;
        this.skillRegistry = new HashMap<>();
        if (skills != null) {
            for (ChatDispatchRequest.SkillDescriptor s : skills) {
                skillRegistry.put(s.getName(), s);
            }
        }
    }

    @Tool(name = "read_skill",
          description = "Read the full instruction content (SKILL.md) of a specified skill. "
                  + "Call this tool first to get detailed execution steps and parameter descriptions before using a skill. "
                  + "The parameter must exactly match a name from the available skills list.")
    public String readSkill(@ToolParam(description = "Skill name, must exactly match a name from the available skills list") String skillName) {
        if (skillName == null || skillName.trim().isEmpty()) {
            return "Error: please provide a skill name";
        }

        ChatDispatchRequest.SkillDescriptor descriptor = skillRegistry.get(skillName.trim());
        if (descriptor == null) {
            return "Error: skill \"" + skillName + "\" not found, available skills: " + String.join(", ", skillRegistry.keySet());
        }

        // 命中缓存直接返回
        String cached = contentCache.get(descriptor.getId());
        if (cached != null) {
            log.debug("read_skill cache hit: skill={}", skillName);
            return cached;
        }

        log.info("read_skill: fetching content for skill={}, id={}", skillName, descriptor.getId());

        // 同步获取 skillContent（这是 LLM 需要立即读取的指令内容）
        SkillContentRequest request = SkillContentRequest.builder()
                .skillId(descriptor.getId())
                .agentId(null)
                .build();
        
        SkillContentResponse response = rpcClient.fetchSkillContent(request);
        if (response == null || response.getSkillContent() == null) {
            return "Error: failed to retrieve skill content, please try again later";
        }

        // 缓存 skillContent
        contentCache.put(descriptor.getId(), response.getSkillContent());

        // 异步写入支撑文件到本地临时目录（不阻塞 read_skill 返回）
        String version = descriptor.getVersion() != null ? descriptor.getVersion() : "1";
        Path skillDir = Path.of(tempDir, String.valueOf(descriptor.getId()), version);
        CompletableFuture.runAsync(() -> {
            try {
                materializeFiles(skillDir, response);
                log.info("Skill files materialized to: {}", skillDir);
            } catch (Exception e) {
                log.warn("Failed to materialize skill files to temp dir: {}", skillDir, e);
            }
        });

        // 立即返回 skillContent（与 Server 端 ReadSkillTool 行为一致）
        return response.getSkillContent();
    }

    /**
     * 获取 skill 临时目录路径（供 ShellTool 使用）
     */
    public String getSkillDir(Long skillId, String version) {
        return Path.of(tempDir, String.valueOf(skillId), version != null ? version : "1").toString();
    }

    private void materializeFiles(Path skillDir, SkillContentResponse response) throws Exception {
        Files.createDirectories(skillDir);

        // 写入 SKILL.md
        Files.writeString(skillDir.resolve("SKILL.md"), response.getSkillContent(), StandardCharsets.UTF_8);

        // 写入支撑文件
        if (response.getFiles() != null) {
            for (SkillContentResponse.SkillFile file : response.getFiles()) {
                Path filePath = skillDir.resolve(file.getFilePath());
                Files.createDirectories(filePath.getParent());

                if ("base64".equals(file.getEncoding())) {
                    byte[] decoded = Base64.getDecoder().decode(file.getContent());
                    Files.write(filePath, decoded);
                } else {
                    Files.writeString(filePath, file.getContent(), StandardCharsets.UTF_8);
                }
            }
        }
    }
}
