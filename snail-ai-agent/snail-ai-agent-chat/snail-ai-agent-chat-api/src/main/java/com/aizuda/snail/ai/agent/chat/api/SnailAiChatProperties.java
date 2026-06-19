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
        private Embed embed = new Embed();
    }

    @Data
    public static class Embed {
        private Boolean enabled;
        private Boolean showHeader;
        private Boolean showSidebarUser;
        private Boolean showAgentMarket;
        private Boolean compactInput;
        private Boolean lockAgent;
    }

    @Data
    public static class Session {
        private Integer tokenTtlSeconds = 3600;
    }
}
