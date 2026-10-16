package com.cms.service;

import com.cms.dto.response.StatsResp;

/**
 * 数据统计服务（文档 5.1 统计接口 / 7.11 统计口径）
 */
public interface StatsService {

    /** 访问统计：PV/UV 按日或按月（days = 最近 N 天） */
    java.util.List<StatsResp.TrendPoint> visits(int days, String granularity);

    /** 文章统计：状态分布 + 热门 TopN（含 Redis 增量） */
    StatsResp.ArticleStats articles(int topN);

    /** 用户统计：总数 + 增长 + 角色分布 */
    StatsResp.UserStats users(int days, String granularity);

    /** 仪表盘概览 */
    StatsResp.Dashboard dashboard();
}
