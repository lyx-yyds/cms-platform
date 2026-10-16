/** API 通用类型（对应文档 types/api.d.ts） */

/** 后端统一返回包装（vo/Result.java） */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页请求参数 */
export interface PageParams {
  page: number
  size: number
}

/** 分页返回 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}
