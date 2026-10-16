/**
 * 数据统计接口（文档 5.1 统计接口 / 7.11 口径，仅 ADMIN）
 */
import { http } from '@/utils/request'

export interface TrendPoint {
  time: string
  pv: number
  uv: number
}

export interface CountPoint {
  time: string
  count: number
}

export interface StatusCount {
  status: number
  count: number
}

export interface TopArticle {
  id: number
  title: string
  views: number
}

export interface RoleCount {
  roleName: string
  count: number
}

export interface ArticleBrief {
  id: number
  title: string
  status: number
  views: number
  time: string
}

export interface ArticleStats {
  statusCount: StatusCount[]
  topArticles: TopArticle[]
}

export interface UserStats {
  total: number
  growth: CountPoint[]
  roleDistribution: RoleCount[]
}

export interface DashboardData {
  articleTotal: number
  articlePublished: number
  articleDraft: number
  articleOffline: number
  userTotal: number
  commentTotal: number
  commentPending: number
  todayPv: number
  todayUv: number
  weekTrend: TrendPoint[]
  pendingArticles: ArticleBrief[]
  recentPublished: ArticleBrief[]
}

/** 访问统计 GET /api/stats/visits */
export function getVisitsApi(days = 30, granularity: 'day' | 'month' = 'day'): Promise<TrendPoint[]> {
  return http.get<TrendPoint[]>('/stats/visits', { days, granularity })
}

/** 文章统计 GET /api/stats/articles */
export function getArticleStatsApi(topN = 10): Promise<ArticleStats> {
  return http.get<ArticleStats>('/stats/articles', { topN })
}

/** 用户统计 GET /api/stats/users */
export function getUserStatsApi(days = 30, granularity: 'day' | 'month' = 'day'): Promise<UserStats> {
  return http.get<UserStats>('/stats/users', { days, granularity })
}

/** 仪表盘 GET /api/stats/dashboard */
export function getDashboardApi(): Promise<DashboardData> {
  return http.get<DashboardData>('/stats/dashboard')
}

export const STATUS_LABEL: Record<number, string> = {
  0: '草稿',
  1: '已发布',
  2: '已下架'
}
