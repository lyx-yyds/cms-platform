/**
 * AI 智能接口（文档 5.1 AI 智能接口 / 7.4）
 * 写作助手：SSE 流式逐字返回；摘要/SEO 同步；生成记录分页
 */
import { http } from '@/utils/request'
import { getAccessToken } from '@/utils/auth'
import type { PageResult } from '@/api/admin'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

export interface AiWriteReq {
  articleId?: number
  title?: string
  keywords?: string
  outline?: string
  content?: string
  instruction?: string
}

export interface AiEnhanceResp {
  summary: string
  seoTitle: string
  seoDescription: string
  seoKeywords: string
}

export interface AiGenerationRecord {
  id: number
  userId: number
  articleId: number | null
  type: string
  prompt: string
  result: string
  model: string
  status: number
  tokensUsed: number
  errorMsg: string | null
  createTime: string
}

export const AI_TYPE_LABEL: Record<string, string> = {
  draft: '草稿生成',
  continue: '续写',
  polish: '润色',
  seo: '摘要+SEO'
}

/** SSE 流式请求：POST + ReadableStream 解析 text/event-stream */
async function aiStream(
  url: string,
  body: AiWriteReq,
  onChunk: (text: string) => void,
  signal?: AbortSignal
): Promise<void> {
  const resp = await fetch(`${API_BASE}${url}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getAccessToken() || ''}`
    },
    body: JSON.stringify(body),
    signal
  })
  // 非 200 或非流式（业务错误 JSON）→ 抛错
  if (!resp.ok) {
    let msg = `请求失败（${resp.status}）`
    try {
      const j = await resp.json()
      if (j.message) msg = j.message
    } catch {
      /* ignore */
    }
    throw new Error(msg)
  }
  const contentType = resp.headers.get('content-type') || ''
  if (!contentType.includes('text/event-stream')) {
    // 业务错误（如限流 2002 以 Result JSON 返回）
    let msg = '生成失败'
    try {
      const j = await resp.json()
      if (j.message) msg = j.message
    } catch {
      /* ignore */
    }
    throw new Error(msg)
  }
  const reader = resp.body?.getReader()
  if (!reader) throw new Error('无法读取响应流')
  const decoder = new TextDecoder()
  let buf = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    const parts = buf.split('\n\n')
    buf = parts.pop() ?? ''
    for (const part of parts) {
      const dataLine = part.split('\n').find((l) => l.startsWith('data:'))
      if (dataLine) {
        const data = dataLine.slice(5).trim()
        if (data && data !== '[DONE]') onChunk(data)
      }
    }
  }
}

/** 生成草稿（SSE） */
export function writeDraftStream(
  req: AiWriteReq,
  onChunk: (t: string) => void,
  signal?: AbortSignal
): Promise<void> {
  return aiStream('/ai/write/draft', req, onChunk, signal)
}

/** 续写（SSE） */
export function writeContinueStream(
  req: AiWriteReq,
  onChunk: (t: string) => void,
  signal?: AbortSignal
): Promise<void> {
  return aiStream('/ai/write/continue', req, onChunk, signal)
}

/** 润色（SSE） */
export function writePolishStream(
  req: AiWriteReq,
  onChunk: (t: string) => void,
  signal?: AbortSignal
): Promise<void> {
  return aiStream('/ai/write/polish', req, onChunk, signal)
}

/** 摘要 + SEO（同步） */
export function enhanceApi(req: { title?: string; content?: string }): Promise<AiEnhanceResp> {
  return http.post<AiEnhanceResp>('/ai/article/enhance', req)
}

/** 我的生成记录（分页） */
export function generationsApi(params: { page: number; size: number }): Promise<PageResult<AiGenerationRecord>> {
  return http.get<PageResult<AiGenerationRecord>>('/ai/generations', params)
}

/** 单条生成记录详情（续编辑保存） */
export function generationDetailApi(id: number): Promise<AiGenerationRecord> {
  return http.get<AiGenerationRecord>(`/ai/generations/${id}`)
}

/** 绑定生成记录到已保存文章（回填 article_id） */
export function bindArticleApi(id: number, articleId: number): Promise<void> {
  return http.post<void>(`/ai/generations/${id}/bind?articleId=${articleId}`)
}
