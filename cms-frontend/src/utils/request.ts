/**
 * axios 请求封装（对应文档 utils/request.ts）
 * 特性：统一解包 ApiResult / 401 自动刷新 AccessToken 并重试 / 刷新失败跳登录
 */
import axios, { type AxiosRequestConfig, type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from './auth'
import type { ApiResult } from '@/types/api'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

/* 请求拦截：携带 AccessToken */
service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/* 刷新锁：并发 401 时只发起一次刷新 */
let refreshing: Promise<boolean> | null = null

async function doRefresh(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false
  try {
    const { data } = await axios.post<ApiResult<{ accessToken: string; refreshToken: string }>>(
      `${import.meta.env.VITE_API_BASE_URL || '/api'}/auth/refresh`,
      { refreshToken }
    )
    if (data.code === 200 && data.data.accessToken) {
      setTokens(data.data)
      return true
    }
    return false
  } catch {
    return false
  }
}

async function refreshAccessToken(): Promise<boolean> {
  if (!refreshing) {
    refreshing = doRefresh().finally(() => {
      refreshing = null
    })
  }
  return refreshing
}

function redirectToLogin(): void {
  clearTokens()
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

/* 响应拦截：统一解包 + 401 刷新重试 */
service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResult
    if (res.code === undefined) {
      // 非标准包装（如文件流），直接返回
      return response.data
    }
    if (res.code === 200) {
      return res.data
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  async (error: AxiosError<ApiResult>) => {
    const original = error.config as (AxiosRequestConfig & { _retried?: boolean }) | undefined
    const status = error.response?.status

    // 401：尝试用 RefreshToken 换新 AccessToken 后重试一次
    if (status === 401 && original && !original._retried) {
      original._retried = true
      const ok = await refreshAccessToken()
      if (ok) {
        return service(original)
      }
    }
    if (status === 401) {
      redirectToLogin()
    }
    const msg = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

/** 泛型请求方法 */
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service(config) as unknown as Promise<T>
}

export const http = {
  get<T = unknown>(url: string, params?: object): Promise<T> {
    return request<T>({ url, method: 'get', params })
  },
  post<T = unknown>(url: string, data?: object): Promise<T> {
    return request<T>({ url, method: 'post', data })
  },
  put<T = unknown>(url: string, data?: object): Promise<T> {
    return request<T>({ url, method: 'put', data })
  },
  delete<T = unknown>(url: string, params?: object): Promise<T> {
    return request<T>({ url, method: 'delete', params })
  }
}

export default service
