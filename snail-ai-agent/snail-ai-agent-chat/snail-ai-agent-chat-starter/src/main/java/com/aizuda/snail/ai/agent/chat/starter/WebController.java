package com.aizuda.snail.ai.agent.chat.starter;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 嵌入式对话页面路由控制器，处理 {@code /snail-chat} 的重定向与首页转发。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Controller
public class WebController {

    private static final String CHAT_PATH = "/snail-chat";
    private static final String CHAT_ROOT_PATH = "/snail-chat/";
    private static final String CHAT_INDEX_FORWARD = "forward:/snail-chat/index.html";
    private static final String CHAT_ROOT_REDIRECT = "redirect:/snail-chat/";
    private static final String QUERY_SEPARATOR = "?";

    @GetMapping(CHAT_PATH)
    public String redirectChatRoot(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (StrUtil.isBlank(queryString)) {
            return CHAT_ROOT_REDIRECT;
        }
        return CHAT_ROOT_REDIRECT + QUERY_SEPARATOR + queryString;
    }

    @GetMapping(CHAT_ROOT_PATH)
    public String forwardChatIndex() {
        return CHAT_INDEX_FORWARD;
    }
}
