package com.aizuda.snail.ai.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 默认接口 — 将 /root 转发到前端单页应用。
 *
 * @author: opensnail
 * @since : 2022-03-28 22:17
 */
@Controller
public class AdminWebController {

    /** 转发到前端单页应用首页（index）。 */
    @GetMapping("/root")
    public String forwardIndex() {
        return "index";
    }

}
