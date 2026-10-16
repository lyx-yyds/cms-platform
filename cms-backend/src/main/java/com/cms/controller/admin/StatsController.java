package com.cms.controller.admin;

import com.cms.common.Result;
import com.cms.dto.response.StatsResp;
import com.cms.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据统计接口（admin/StatsController.java，文档 5.1 / 7.11，仅 ADMIN）
 */
@Tag(name = "管理端-数据统计")
@RestController("statsController")
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('stats:view')")
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "访问统计（PV/UV，按日或按月）")
    @GetMapping("/visits")
    public Result<List<StatsResp.TrendPoint>> visits(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "day") String granularity) {
        return Result.ok(statsService.visits(days, granularity));
    }

    @Operation(summary = "文章统计（状态分布 + 热门 TopN）")
    @GetMapping("/articles")
    public Result<StatsResp.ArticleStats> articles(@RequestParam(defaultValue = "10") int topN) {
        return Result.ok(statsService.articles(topN));
    }

    @Operation(summary = "用户统计（总数 + 增长 + 角色分布）")
    @GetMapping("/users")
    public Result<StatsResp.UserStats> users(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "day") String granularity) {
        return Result.ok(statsService.users(days, granularity));
    }

    @Operation(summary = "仪表盘概览")
    @GetMapping("/dashboard")
    public Result<StatsResp.Dashboard> dashboard() {
        return Result.ok(statsService.dashboard());
    }
}
