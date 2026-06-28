package com.aizuda.snail.ai.agent.core.resolver;

/**
 * Skill 渐进式披露 — 系统提示词模板常量 + 轻量级技能列表构建
 */
public final class SkillPromptConstants {

    private SkillPromptConstants() {}

    /** 渐进式披露系统提示词模板，{skills_list} 为占位符 */
    public static final String SYSTEM_PROMPT_TEMPLATE = """

            ## Skills System

            你可以使用技能库来获得专项能力和领域知识。

            ### 可用技能

            {skills_list}

            ### 使用方式（渐进式披露）

            技能遵循**渐进式披露**模式 — 你已知每个技能的名称和描述，但需要按需读取完整指令：

            1. **识别技能**: 判断用户的问题是否匹配某个技能的描述
            2. **读取指令**: 使用 `read_skill` 工具传入技能名称，获取完整的 SKILL.md 指令
            3. **执行指令**: 按照 SKILL.md 中的步骤、工作流和最佳实践执行
            4. **访问支撑文件**: 技能可能包含脚本、配置或参考文档，使用 `shell` 工具配合绝对路径访问
            5. **调用远程接口**: 如需请求外部 API，使用 `http_request` 工具发起 GET/POST 请求

            **重要**:
            - 始终通过 `read_skill` 工具读取技能指令，不要尝试通过其他方式访问 SKILL.md
            - 对于技能中的其他支撑文件（脚本、参考资料等），可使用 `shell` 工具配合绝对路径访问
            - 需要调用远程接口时，优先使用 `http_request` 工具，无需编写 curl 脚本

            ### 何时使用技能

            - 当用户的请求匹配某个技能的领域时（如 "提取PDF内容" → pdf-extractor 技能）
            - 当你需要专业知识或结构化工作流时
            - 当某个技能为复杂任务提供了经过验证的模式时

            ### 技能自文档化

            - 每个 SKILL.md 会告诉你该技能的确切功能和使用方法
            - 上方技能列表中显示了每个技能的支撑文件目录路径

            ### 执行技能脚本

            技能可能包含 Python 脚本或其他可执行文件，始终使用技能列表中的绝对路径来执行。

            ### 示例工作流

            用户: "帮我从这个PDF中提取关键信息"

            1. 查看上方可用技能 → 发现 "pdf-extractor" 技能
            2. 使用 `read_skill("pdf-extractor")` 读取完整指令
            3. 按照技能中的提取工作流执行（解析 → 提取 → 整理）
            4. 如有辅助脚本，使用 `shell` 工具配合绝对路径执行

            记住：技能是让你更强大和一致的工具。遇到任务时，先检查是否有匹配的技能！
            """;
}
