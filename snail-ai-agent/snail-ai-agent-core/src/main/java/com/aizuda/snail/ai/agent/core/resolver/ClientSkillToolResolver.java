package com.aizuda.snail.ai.agent.core.resolver;

import com.aizuda.snail.ai.agent.common.rpc.RpcClient;
import com.aizuda.snail.ai.common.dto.agent.ChatDispatchRequest;
import com.aizuda.snail.ai.agent.core.tool.HttpTool;
import com.aizuda.snail.ai.agent.core.tool.ReadSkillTool;
import com.aizuda.snail.ai.agent.core.tool.ShellTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 客户端 Skill 工具解析器
 * <p>
 * 注册 3 个工具（与 Server 端一致）：
 * <ul>
 *   <li>{@code read_skill} — 通过 gRPC 回调 Server 获取 skill 完整内容 + 文件</li>
 *   <li>{@code shell} — 在 skill 临时目录中执行 shell 命令</li>
 *   <li>{@code http_request} — 发起 HTTP 请求</li>
 * </ul>
 *
 * @author opensnail
 */
@Slf4j
public class ClientSkillToolResolver {

    private static final long DEFAULT_SHELL_TIMEOUT_MS = 60000;
    private static final int DEFAULT_SHELL_MAX_OUTPUT_LINES = 500;
    private static final long DEFAULT_HTTP_TIMEOUT_MS = 30000;

    private final RpcClient rpcClient;
    private final String skillTempDir;

    public ClientSkillToolResolver(RpcClient rpcClient, String skillTempDir) {
        this.rpcClient = rpcClient;
        this.skillTempDir = skillTempDir;
    }

    /**
     * 从 Skill 描述符列表构建工具回调
     */
    public List<ToolCallback> resolve(ChatDispatchRequest request) {
        List<ChatDispatchRequest.SkillDescriptor> skills = request.getSkills();
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }

        List<ToolCallback> callbacks = new ArrayList<>();
        try {
            String skillPrompt = SkillPromptConstants.SYSTEM_PROMPT_TEMPLATE
                    .replace("{skills_list}", buildSkillsList(skills));
            request.setSystemPrompt(request.getSystemPrompt() + skillPrompt);

            callbacks.addAll(Arrays.asList(ToolCallbacks.from(
                    new ReadSkillTool(skills, rpcClient, skillTempDir),
                    new ShellTool(skillTempDir, DEFAULT_SHELL_TIMEOUT_MS, DEFAULT_SHELL_MAX_OUTPUT_LINES),
                    new HttpTool(DEFAULT_HTTP_TIMEOUT_MS)
            )));
            log.info("Skill tools resolved: {} skills, ReadSkillTool + ShellTool + HttpTool registered", skills.size());
        } catch (Exception e) {
            log.warn("Failed to resolve skill tools", e);
        }

        return callbacks;
    }

    /**
     * 构建轻量级技能列表（仅 name + description + filePath），不含完整 skillContent
     */
    public String buildSkillsList(List<ChatDispatchRequest.SkillDescriptor> skills) {
        StringBuilder sb = new StringBuilder();
        for (ChatDispatchRequest.SkillDescriptor skill : skills) {
            sb.append("- **").append(skill.getName()).append("**");
            if (skill.getDescription() != null) {
                sb.append(": ").append(skill.getDescription());
            }
            sb.append(" → 支撑文件目录: `").append(getSkillTempDir(skill)).append("`");
            sb.append("\n");
        }
        return sb.toString();
    }

    public Path getSkillTempDir(ChatDispatchRequest.SkillDescriptor po) {
        return Path.of(skillTempDir + File.separator + po.getId() + File.separator + po.getVersion());
    }
}
