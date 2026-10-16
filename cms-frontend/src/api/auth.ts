/**
 * 认证接口（对应文档 api/auth.ts，接口见文档 §5.1 认证接口）
 */
import { http } from '@/utils/request'
import type { LoginParams, LoginResult, RegisterParams, UserInfo } from '@/types/user'

/** 用户登录 POST /api/auth/login */
export function loginApi(data: LoginParams): Promise<LoginResult> {
  return http.post<LoginResult>('/auth/login', data)
}

/** 用户注册 POST /api/auth/register */
export function registerApi(data: RegisterParams): Promise<void> {
  return http.post<void>('/auth/register', data)
}

/** 退出登录 POST /api/auth/logout（注销 RefreshToken） */
export function logoutApi(): Promise<void> {
  return http.post<void>('/auth/logout')
}

/** 当前用户信息 GET /api/user/info */
export function userInfoApi(): Promise<UserInfo> {
  return http.get<UserInfo>('/user/info')
}

/** 修改个人信息 PUT /api/user/info（昵称/邮箱/头像，部分更新） */
export function updateUserInfoApi(data: { nickname?: string; email?: string; avatar?: string }): Promise<void> {
  return http.put<void>('/user/info', data)
}

/** 修改密码 POST /api/user/change-password（校验原密码，无需邮件） */
export function changePasswordApi(data: { oldPassword: string; newPassword: string }): Promise<void> {
  return http.post<void>('/user/change-password', data)
}

/** 刷新 Token POST /api/auth/refresh */
export function refreshApi(data: { refreshToken: string }): Promise<LoginResult> {
  return http.post<LoginResult>('/auth/refresh', data)
}

/** 忘记密码 POST /api/auth/forgot-password（v2.1） */
export function forgotPasswordApi(data: { email: string }): Promise<void> {
  return http.post<void>('/auth/forgot-password', data)
}

/** 重置密码 POST /api/auth/reset-password（v2.1） */
export function resetPasswordApi(data: { token: string; password: string }): Promise<void> {
  return http.post<void>('/auth/reset-password', data)
}
