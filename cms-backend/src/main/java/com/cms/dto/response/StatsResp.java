package com.cms.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 统计响应（文档 7.11 统计口径）
 */
@Data
public class StatsResp {

    /** 访问趋势点（PV/UV，时间维度按日或月） */
    @Data
    public static class TrendPoint {
        private String time;
        private long pv;
        private long uv;
    }

    /** 计数趋势点（用户增长） */
    @Data
    public static class CountPoint {
        private String time;
        private long count;
    }

    /** 文章状态分布（0草稿/1发布/2下架） */
    @Data
    public static class StatusCount {
        private Integer status;
        private long count;
    }

    /** 热门文章（含 Redis 未落库增量，文档 7.11） */
    @Data
    public static class TopArticle {
        private Long id;
        private String title;
        private long views;
    }

    /** 角色分布 */
    @Data
    public static class RoleCount {
        private String roleName;
        private long count;
    }

    /** 文章简项（仪表盘待审/最近发布） */
    @Data
    public static class ArticleBrief {
        private Long id;
        private String title;
        private Integer status;
        private long views;
        private String time;
    }

    /** 文章统计响应 */
    @Data
    public static class ArticleStats {
        private List<StatusCount> statusCount;
        private List<TopArticle> topArticles;
    }

    /** 用户统计响应 */
    @Data
    public static class UserStats {
        private long total;
        private List<CountPoint> growth;
        private List<RoleCount> roleDistribution;
    }

    /** 仪表盘概览 */
    @Data
    public static class Dashboard {
        private long articleTotal;
        private long articlePublished;
        private long articleDraft;
        private long articleOffline;
        private long userTotal;
        private long commentTotal;
        private long commentPending;
        private long todayPv;
        private long todayUv;
        private List<TrendPoint> weekTrend;
        private List<ArticleBrief> pendingArticles;
        private List<ArticleBrief> recentPublished;
    }
}
