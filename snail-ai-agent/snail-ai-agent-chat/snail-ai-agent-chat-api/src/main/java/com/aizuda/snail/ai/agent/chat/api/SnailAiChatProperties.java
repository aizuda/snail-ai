package com.aizuda.snail.ai.agent.chat.api;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "snail-ai.chat")
public class SnailAiChatProperties {

    public static final String DEFAULT_PAGE_TITLE = "Snail AI Chat";

    private boolean enabled = true;

    private Ui ui = new Ui();

    private Session session = new Session();

    @Data
    public static class Ui {
        private String pageTitle = DEFAULT_PAGE_TITLE;
        private String logo = "https://snailjob.opensnail.com/logo.svg";
        private String resourceBaseUrl;
        private Embed embed = new Embed();
    }

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
    }

    @Data
    public static class Session {
        private Integer tokenTtlSeconds = 3600;
    }
}
