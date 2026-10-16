/**
 * 文章 / 分类 / 标签接口（对应文档 §5.1 第三阶段）
 */
import { http } from '@/utils/request'
import type { PageResult } from '@/api/admin'

export interface ArticleItem {
  id: number
  title: string
  summary?: string
  coverImage?: string
  categoryId?: number
  categoryName?: string
  authorId?: number
  authorName?: string
  /** 0-草稿 1-已发布 2-已下架 */
  status: number
  isTop: boolean
  isRecommend: boolean
  viewCount: number
  likeCount: number
  commentCount: number
  publishTime?: string
  createTime?: string
  tags: string[]
}

export interface ArticleDetail extends ArticleItem {
  content: string
  seoTitle?: string
  seoDescription?: string
  seoKeywords?: string
  liked: boolean
}

export interface ArticleForm {
  title: string
  content?: string
  summary?: string
  coverImage?: string
  categoryId?: number
  status?: number
  isTop?: boolean
  isRecommend?: boolean
  tagIds?: number[]
  seoTitle?: string
  seoDescription?: string
  seoKeywords?: string
}

/** 我的文章分页 GET /api/articles/mine */
export function pageMineArticlesApi(params: {
  page: number
  size: number
  keyword?: string
  status?: number
}): Promise<PageResult<ArticleItem>> {
  return http.get<PageResult<ArticleItem>>('/articles/mine', params)
}

/** 公开文章分页 GET /api/articles */
export function pageArticlesApi(params: {
  page: number
  size: number
  keyword?: string
  categoryId?: number
  status?: number
}): Promise<PageResult<ArticleItem>> {
  return http.get<PageResult<ArticleItem>>('/articles', params)
}

/** 文章详情 GET /api/articles/{id} */
export function getArticleApi(id: number): Promise<ArticleDetail> {
  return http.get<ArticleDetail>(`/articles/${id}`)
}

/** 创建文章 POST /api/articles */
export function createArticleApi(data: ArticleForm): Promise<number> {
  return http.post<number>('/articles', data)
}

/** 修改文章 PUT /api/articles/{id} */
export function updateArticleApi(id: number, data: ArticleForm): Promise<void> {
  return http.put<void>(`/articles/${id}`, data)
}

/** 删除文章 DELETE /api/articles/{id} */
export function deleteArticleApi(id: number): Promise<void> {
  return http.delete<void>(`/articles/${id}`)
}

/** 点赞 POST /api/articles/{id}/like */
export function likeArticleApi(id: number): Promise<void> {
  return http.post<void>(`/articles/${id}/like`)
}

/** 取消点赞 DELETE /api/articles/{id}/like */
export function unlikeArticleApi(id: number): Promise<void> {
  return http.delete<void>(`/articles/${id}/like`)
}

// ===== 分类 =====

export interface CategoryItem {
  id: number
  name: string
  slug?: string
  description?: string
  sortOrder: number
  status: number
  articleCount: number
}

export interface CategoryForm {
  name: string
  slug?: string
  description?: string
  parentId?: number
  sortOrder?: number
  status?: number
}

/** 分类列表 GET /api/categories（公开） */
export function listCategoriesApi(): Promise<CategoryItem[]> {
  return http.get<CategoryItem[]>('/categories')
}

/** 新增分类 POST /api/categories（ADMIN） */
export function createCategoryApi(data: CategoryForm): Promise<void> {
  return http.post<void>('/categories', data)
}

/** 修改分类 PUT /api/categories/{id}（ADMIN） */
export function updateCategoryApi(id: number, data: CategoryForm): Promise<void> {
  return http.put<void>(`/categories/${id}`, data)
}

/** 删除分类 DELETE /api/categories/{id}（ADMIN） */
export function deleteCategoryApi(id: number): Promise<void> {
  return http.delete<void>(`/categories/${id}`)
}

// ===== 标签 =====

export interface TagItem {
  id: number
  name: string
  slug?: string
  articleCount: number
}

export interface TagForm {
  name: string
  slug?: string
}

/** 标签列表 GET /api/tags（公开） */
export function listTagsApi(): Promise<TagItem[]> {
  return http.get<TagItem[]>('/tags')
}

/** 新增标签 POST /api/tags（ADMIN） */
export function createTagApi(data: TagForm): Promise<void> {
  return http.post<void>('/tags', data)
}

/** 修改标签 PUT /api/tags/{id}（ADMIN） */
export function updateTagApi(id: number, data: TagForm): Promise<void> {
  return http.put<void>(`/tags/${id}`, data)
}

/** 删除标签 DELETE /api/tags/{id}（ADMIN） */
export function deleteTagApi(id: number): Promise<void> {
  return http.delete<void>(`/tags/${id}`)
}
