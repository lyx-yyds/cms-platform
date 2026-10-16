/**
 * 媒体接口（对应文档 §5.1 媒体接口 / §7.8 上传安全）
 */
import { http } from '@/utils/request'
import type { PageResult } from '@/api/admin'

export interface MediaItem {
  id: number
  name: string
  originalName: string
  url: string
  type?: string
  size: number
  extension?: string
  uploadUserId: number
  uploadUserNickname?: string
  createTime?: string
}

/** 上传文件 POST /api/media/upload（multipart） */
export function uploadMediaApi(file: File): Promise<MediaItem> {
  const fd = new FormData()
  fd.append('file', file)
  return http.post<MediaItem>('/media/upload', fd as unknown as object)
}

/** 文件分页 GET /api/media */
export function pageMediaApi(params: {
  page: number
  size: number
  type?: string
  keyword?: string
}): Promise<PageResult<MediaItem>> {
  return http.get<PageResult<MediaItem>>('/media', params)
}

/** 删除文件 DELETE /api/media/{id}（本人或 ADMIN） */
export function deleteMediaApi(id: number): Promise<void> {
  return http.delete<void>(`/media/${id}`)
}

/** 文件大小展示 */
export function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let v = bytes
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${v >= 100 ? Math.round(v) : v.toFixed(1)} ${units[i]}`
}
