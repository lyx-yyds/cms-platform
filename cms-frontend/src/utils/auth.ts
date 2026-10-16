/**
 * 认证工具（对应文档 utils/auth.ts）
 * Token 双令牌：accessToken + refreshToken
 */
import { getStorage, setStorage, removeStorage } from './storage'

const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

export interface TokenPair {
  accessToken: string
  refreshToken: string
  tokenType?: string
  expiresIn?: number
}

export function getAccessToken(): string | null {
  return getStorage<string>(ACCESS_TOKEN_KEY)
}

export function getRefreshToken(): string | null {
  return getStorage<string>(REFRESH_TOKEN_KEY)
}

export function setTokens(tokens: TokenPair): void {
  setStorage(ACCESS_TOKEN_KEY, tokens.accessToken)
  setStorage(REFRESH_TOKEN_KEY, tokens.refreshToken)
}

export function clearTokens(): void {
  removeStorage(ACCESS_TOKEN_KEY)
  removeStorage(REFRESH_TOKEN_KEY)
}

export function isLoggedIn(): boolean {
  return !!getAccessToken()
}
