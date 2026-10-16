/**
 * 评论接口（对应文档 §5.1 评论接口 / §7.5 AI 机审）
 */
import { http } from '@/utils/request'
import type { PageResult } from '@/api/admin'

export interface CommentItem {
  id: number
  articleId: number
  articleTitle?: string
  userId: number
  userNickname?: string
  parentId?: number
  replyUserId?: number
  replyNickname?: string
  content: string
  /** 0-待审核 1-已通过 2-已拒绝 */
  status: number
  /** AI 机审状态：0-未机审 1-机审通过 2-转人工 3-机审拒绝 */
  aiStatus: number
  ipAddress?: string
  createTime?: string
  /** AI 机审明细（来自 ai_review_record） */
  aiResult?: string
  aiScore?: number
  aiReason?: string
}

/** 评论分页 GET /api/admin/comments（ADMIN，含机审状态） */
export function pageCommentsApi(params: {
  page: number
  size: number
  articleId?: number
  status?: number
  aiStatus?: number
  keyword?: string
}): Promise<PageResult<CommentItem>> {
  return http.get<PageResult<CommentItem>>('/admin/comments', params)
}

/** 人工复核 PUT /api/admin/comments/{id}/audit（ADMIN，status: 1通过 2拒绝） */
export function auditCommentApi(id: number, status: number): Promise<void> {
  return http.put<void>(`/admin/comments/${id}/audit`, { status })
}

/** 删除评论 DELETE /api/comments/{id}（本人或 ADMIN） */
export function deleteCommentApi(id: number): Promise<void> {
  return http.delete<void>(`/comments/${id}`)
}

/** 文章评论列表 GET /api/articles/{articleId}/comments（公开，仅已通过） */
export function listArticleCommentsApi(articleId: number): Promise<CommentItem[]> {
  return http.get<CommentItem[]>(`/articles/${articleId}/comments`)
}

/** 发表评论 POST /api/articles/{articleId}/comments（触发 AI 初审） */
export function createCommentApi(articleId: number, data: { content: string; parentId?: number; replyUserId?: number }): Promise<number> {
  return http.post<number>(`/articles/${articleId}/comments`, data)
}
