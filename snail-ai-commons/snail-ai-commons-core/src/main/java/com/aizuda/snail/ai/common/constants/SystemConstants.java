package com.aizuda.snail.ai.common.constants;

/**
 * 系统通用常量
 */
public interface SystemConstants {

    /**
     * 长时间格式
     */
    String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    /**
     * 短时间格式
     */
    String YYYY_MM_DD = "yyyy-MM-dd";

    String SKILL_MD = "SKILL.md";

    /**
     * RAG MCP 服务器名称常量
     */
    String RAG_MCP_SERVER_NAME = "snail-ai-rag-mcp-server";

    String LOGO = """
            _____                   _   _                _\s
             / ____|                 (_) | |       /\\     (_)
            | (___    _ __     __ _   _  | |      /  \\     _\s
             \\___ \\  | '_ \\   / _` | | | | |     / /\\ \\   | |
             ____) | | | | | | (_| | | | | |    / ____ \\  | |
            |_____/  |_| |_|  \\__,_| |_| |_|   /_/    \\_\\ |_|
            :: Snail Ai ::                                 (v{}) \s
            """;
}
