package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cms.common.Constants;
import com.cms.dto.response.StatsResp;
import com.cms.entity.Article;
import com.cms.entity.Comment;
import com.cms.mapper.ArticleMapper;
import com.cms.mapper.CommentMapper;
import com.cms.mapper.UserMapper;
import com.cms.mapper.VisitLogMapper;
import com.cms.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据统计服务实现（文档 7.11 口径：PV 原始计数 / UV 按 user_id+ip 去重）
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final VisitLogMapper visitLogMapper;
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<StatsResp.TrendPoint> visits(int days, String granularity) {
        boolean byMonth = "month".equalsIgnoreCase(granularity);
        LocalDate start = LocalDate.now().minusDays(Math.max(1, days) - 1);
        List<Map<String, Object>> rows = byMonth
                ? visitLogMapper.selectPvUvByMonth(start)
                : visitLogMapper.selectPvUvByDay(start);
        Map<String, StatsResp.TrendPoint> byKey = rows.stream().map(row -> {
            StatsResp.TrendPoint p = new StatsResp.TrendPoint();
            p.setTime(String.valueOf(row.get("time")));
            p.setPv(((Number) row.get("pv")).longValue());
            p.setUv(((Number) row.get("uv")).longValue());
            return p;
        }).collect(Collectors.toMap(StatsResp.TrendPoint::getTime, p -> p));
        return fillTrend(days, start, byMonth, byKey);
    }

    /** 按时间维度补零，保证日期/月份连续（口径与 dashboard 7 日趋势一致） */
    private List<StatsResp.TrendPoint> fillTrend(int days, LocalDate start, boolean byMonth,
                                                 Map<String, StatsResp.TrendPoint> byKey) {
        List<StatsResp.TrendPoint> list = new ArrayList<>();
        if (byMonth) {
            YearMonth ym = YearMonth.from(start);
            YearMonth end = YearMonth.from(LocalDate.now());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
            while (!ym.isAfter(end)) {
                String key = ym.format(fmt);
                StatsResp.TrendPoint p = byKey.get(key);
                if (p == null) {
                    p = new StatsResp.TrendPoint();
                    p.setTime(key);
                    p.setPv(0);
                    p.setUv(0);
                }
                list.add(p);
                ym = ym.plusMonths(1);
            }
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (int i = 0; i < days; i++) {
                String key = start.plusDays(i).format(fmt);
                StatsResp.TrendPoint p = byKey.get(key);
                if (p == null) {
                    p = new StatsResp.TrendPoint();
                    p.setTime(key);
                    p.setPv(0);
                    p.setUv(0);
                }
                list.add(p);
            }
        }
        return list;
    }

    @Override
    public StatsResp.ArticleStats articles(int topN) {
        StatsResp.ArticleStats stats = new StatsResp.ArticleStats();
        List<Map<String, Object>> statusRows = visitLogMapper.selectArticleStatusCount();
        stats.setStatusCount(statusRows.stream().map(row -> {
            StatsResp.StatusCount sc = new StatsResp.StatusCount();
            sc.setStatus(((Number) row.get("status")).intValue());
            sc.setCount(((Number) row.get("count")).longValue());
            return sc;
        }).collect(Collectors.toList()));

        List<Article> top = articleMapper.selectList(new QueryWrapper<Article>()
                .eq("is_deleted", 0)
                .eq("status", Constants.ARTICLE_STATUS_PUBLISHED)
                .orderByDesc("view_count")
                .last("LIMIT " + Math.min(Math.max(topN, 1), 20)));
        List<StatsResp.TopArticle> topList = new ArrayList<>();
        for (Article a : top) {
            StatsResp.TopArticle t = new StatsResp.TopArticle();
            t.setId(a.getId());
            t.setTitle(a.getTitle());
            t.setViews(a.getViewCount() + redisViewDelta(a.getId()));
            topList.add(t);
        }
        stats.setTopArticles(topList);
        return stats;
    }

    @Override
    public StatsResp.UserStats users(int days, String granularity) {
        StatsResp.UserStats stats = new StatsResp.UserStats();
        stats.setTotal(userMapper.selectCount(null));
        LocalDate start = LocalDate.now().minusDays(Math.max(1, days) - 1);
        boolean byMonth = "month".equalsIgnoreCase(granularity);
        List<Map<String, Object>> growthRows = byMonth
                ? visitLogMapper.selectUserGrowthByMonth(start)
                : visitLogMapper.selectUserGrowthByDay(start);
        Map<String, StatsResp.CountPoint> byKey = growthRows.stream().map(row -> {
            StatsResp.CountPoint cp = new StatsResp.CountPoint();
            cp.setTime(String.valueOf(row.get("time")));
            cp.setCount(((Number) row.get("count")).longValue());
            return cp;
        }).collect(Collectors.toMap(StatsResp.CountPoint::getTime, cp -> cp));
        List<StatsResp.CountPoint> growth = new ArrayList<>();
        if (byMonth) {
            YearMonth ym = YearMonth.from(start);
            YearMonth end = YearMonth.from(LocalDate.now());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
            while (!ym.isAfter(end)) {
                String key = ym.format(fmt);
                StatsResp.CountPoint cp = byKey.get(key);
                if (cp == null) {
                    cp = new StatsResp.CountPoint();
                    cp.setTime(key);
                    cp.setCount(0);
                }
                growth.add(cp);
                ym = ym.plusMonths(1);
            }
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (int i = 0; i < days; i++) {
                String key = start.plusDays(i).format(fmt);
                StatsResp.CountPoint cp = byKey.get(key);
                if (cp == null) {
                    cp = new StatsResp.CountPoint();
                    cp.setTime(key);
                    cp.setCount(0);
                }
                growth.add(cp);
            }
        }
        stats.setGrowth(growth);
        stats.setRoleDistribution(visitLogMapper.selectRoleDistribution().stream().map(row -> {
            StatsResp.RoleCount rc = new StatsResp.RoleCount();
            rc.setRoleName(String.valueOf(row.get("role_name")));
            rc.setCount(((Number) row.get("count")).longValue());
            return rc;
        }).collect(Collectors.toList()));
        return stats;
    }

    @Override
    public StatsResp.Dashboard dashboard() {
        StatsResp.Dashboard d = new StatsResp.Dashboard();

        // 文章：总数 + 状态分布
        long articleTotal = articleMapper.selectCount(new QueryWrapper<Article>().eq("is_deleted", 0));
        d.setArticleTotal(articleTotal);
        List<Map<String, Object>> statusRows = visitLogMapper.selectArticleStatusCount();
        for (Map<String, Object> row : statusRows) {
            int status = ((Number) row.get("status")).intValue();
            long count = ((Number) row.get("count")).longValue();
            if (status == Constants.ARTICLE_STATUS_PUBLISHED) d.setArticlePublished(count);
            else if (status == Constants.ARTICLE_STATUS_DRAFT) d.setArticleDraft(count);
            else if (status == Constants.ARTICLE_STATUS_OFFLINE) d.setArticleOffline(count);
        }

        // 用户
        d.setUserTotal(userMapper.selectCount(null));

        // 评论：总数 + 待审
        d.setCommentTotal(commentMapper.selectCount(null));
        d.setCommentPending(commentMapper.selectCount(new QueryWrapper<Comment>().eq("status", Constants.COMMENT_STATUS_PENDING)));

        // 今日 PV/UV
        Map<String, Object> today = visitLogMapper.selectPvUvByDay(LocalDate.now()).stream()
                .findFirst().orElse(null);
        if (today != null) {
            d.setTodayPv(((Number) today.get("pv")).longValue());
            d.setTodayUv(((Number) today.get("uv")).longValue());
        }

        // 近 7 日趋势
        List<StatsResp.TrendPoint> week = new ArrayList<>();
        Map<String, StatsResp.TrendPoint> byDay = visits(7, "day").stream()
                .collect(Collectors.toMap(StatsResp.TrendPoint::getTime, p -> p));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            String key = LocalDate.now().minusDays(i).format(fmt);
            StatsResp.TrendPoint p = byDay.get(key);
            if (p == null) {
                p = new StatsResp.TrendPoint();
                p.setTime(key);
                p.setPv(0);
                p.setUv(0);
            }
            week.add(p);
        }
        d.setWeekTrend(week);

        // 待审稿件（草稿，按最近更新）
        List<Article> drafts = articleMapper.selectList(new QueryWrapper<Article>()
                .eq("is_deleted", 0).eq("status", Constants.ARTICLE_STATUS_DRAFT)
                .orderByDesc("update_time").last("LIMIT 6"));
        d.setPendingArticles(briefs(drafts));

        // 最近发布
        List<Article> published = articleMapper.selectList(new QueryWrapper<Article>()
                .eq("is_deleted", 0).eq("status", Constants.ARTICLE_STATUS_PUBLISHED)
                .orderByDesc("publish_time").last("LIMIT 6"));
        d.setRecentPublished(briefs(published));
        return d;
    }

    private List<StatsResp.ArticleBrief> briefs(List<Article> articles) {
        List<StatsResp.ArticleBrief> list = new ArrayList<>();
        for (Article a : articles) {
            StatsResp.ArticleBrief b = new StatsResp.ArticleBrief();
            b.setId(a.getId());
            b.setTitle(a.getTitle());
            b.setStatus(a.getStatus());
            b.setViews(a.getViewCount() + redisViewDelta(a.getId()));
            b.setTime(a.getPublishTime() != null
                    ? a.getPublishTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                    : (a.getUpdateTime() != null
                        ? a.getUpdateTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                        : ""));
            list.add(b);
        }
        return list;
    }

    /** Redis 中未落库的浏览量增量（文档 7.7.1 展示口径：DB + Redis 增量） */
    private long redisViewDelta(Long articleId) {
        try {
            String v = stringRedisTemplate.opsForValue().get(Constants.REDIS_KEY_ARTICLE_VIEWS + articleId);
            return v == null ? 0 : Long.parseLong(v);
        } catch (Exception e) {
            return 0;
        }
    }
}
