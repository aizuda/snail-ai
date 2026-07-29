package com.aizuda.snail.ai.agent.chat.api;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 嵌入式对话页面配置，绑定 {@code snail-ai.chat} 前缀，控制页面 UI 展示与会话令牌有效期。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Data
@ConfigurationProperties(prefix = "snail-ai.chat")
public class SnailAiChatProperties {

    public static final String DEFAULT_PAGE_TITLE = "Snail AI Chat";


    /**
     * 页面 UI 配置
     */
    private Ui ui = new Ui();

    /**
     * 会话配置
     */
    private Session session = new Session();

    /**
     * 页面 UI 配置
     */
    @Data
    public static class Ui {
        /** 页面标题 */
        private String pageTitle = DEFAULT_PAGE_TITLE;
        /** 页面 Logo 地址 */
        private String logo = "https://snailjob.opensnail.com/logo.svg";
        /** 资源访问基础 URL，为空时按 OpenAPI 服务地址推导 */
        private String resourceBaseUrl;
        /** 嵌入模式配置 */
        private Embed embed = new Embed();
    }

    /**
     * 嵌入模式 UI 配置
     */
    @Data
    public static class Embed {
        /**
         * 是否启用嵌入模式；为空时前端按非嵌入模式渲染，可通过 URL 参数覆盖。
         */
        private Boolean enabled;

        /**
         * 嵌入模式下是否显示顶部栏；为空时前端使用嵌入模式默认值。
         */
        private Boolean showHeader;

        /**
         * 嵌入模式下是否显示侧边栏用户信息区域；为空时前端使用嵌入模式默认值。
         */
        private Boolean showSidebarUser;

        /**
         * 嵌入模式下是否显示智能体市场入口；锁定智能体时前端会强制隐藏。
         */
        private Boolean showAgentMarket;

        /**
         * 嵌入模式下是否使用紧凑输入框；为空时前端使用嵌入模式默认值。
         */
        private Boolean compactInput;

        /**
         * 是否锁定当前智能体，锁定后不允许切换智能体或打开智能体市场。
         */
        private Boolean lockAgent;

        /**
         * 嵌入模式下默认选中的智能体 ID；为空时前端按默认逻辑选择。
         * 当 lockAgent=true 时，前端锁定到此智能体并禁止切换。
         */
        private Long defaultAgentId;

        /**
         * 嵌入模式下是否显示"我的智能体"入口。
         * 当 lockAgent=true 时，前端强制隐藏，忽略此配置。
         */
        private Boolean showMyAgents;
    }

    /**
     * 会话配置
     */
    @Data
    public static class Session {
        /** 会话令牌有效期（秒） */
        private Integer tokenTtlSeconds = 3600;
    }
}
