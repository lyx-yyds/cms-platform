/** 用户相关类型（对应文档 types/user.d.ts） */

/** 登录请求（LoginReq） */
export interface LoginParams {
  username: string
  password: string
}

/** 注册请求（RegisterReq） */
export interface RegisterParams {
  username: string
  password: string
  email?: string
}

/** 登录响应（LoginResp，含双令牌） */
export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

/** 用户信息（UserInfoResp） */
export interface UserInfo {
  id: number
  username: string
  nickname?: string
  email?: string
  avatar?: string
  roles: string[]
  permissions?: string[]
}
