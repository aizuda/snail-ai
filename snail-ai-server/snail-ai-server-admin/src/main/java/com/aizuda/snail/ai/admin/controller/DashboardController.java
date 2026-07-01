package com.aizuda.snail.ai.admin.controller;

import com.aizuda.snail.ai.admin.security.annotation.LoginRequired;
import com.aizuda.snail.ai.admin.service.dashboard.DashboardService;
import com.aizuda.snail.ai.admin.vo.dashboard.DashboardOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页运营概览控制器：单接口聚合首页全部数据。
 *
 * @author opensnail
 * @since 2026-06-30
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 首页运营概览（管理员）。一次返回 KPI/趋势/排行/最近对话/知识库/技能/资源。
     * GET /dashboard/overview?range=7d
     */
    @GetMapping("/overview")
    @LoginRequired
    public DashboardOverviewVO overview(@RequestParam(value = "range", defaultValue = "7d") String range) {
        return dashboardService.getOverview(range);
    }
}
