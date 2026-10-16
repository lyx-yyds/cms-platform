/**
 * 用户状态（对应文档 store/user.ts）
 */
import { defineStore } from 'pinia'
import { loginApi, logoutApi, userInfoApi } from '@/api/auth'
import { setTokens, clearTokens, getAccessToken } from '@/utils/auth'
import type { LoginParams, UserInfo } from '@/types/user'

interface UserState {
  token: string | null
  userInfo: UserInfo | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: getAccessToken(),
    userInfo: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    displayName: (state) => state.userInfo?.nickname || state.userInfo?.username || '未登录',
    /** 当前用户角色列表（/api/user/info 返回 roles） */
    roles: (state): string[] => state.userInfo?.roles ?? [],
    /** 当前用户权限点列表（/api/user/info 返回 permissions，随角色权限编辑实时生效） */
    permissions: (state): string[] => state.userInfo?.permissions ?? [],
    /** 是否管理员（ADMIN 角色） */
    isAdmin: (state): boolean => (state.userInfo?.roles ?? []).includes('ADMIN'),
    /** 是否创作角色：拥有任一内容创作/管理权限点（USER 默认仅 article:list，不算） */
    isCreator: (state): boolean => {
      const perms = state.userInfo?.permissions ?? []
      const creatorPerms = [
        'article:create', 'article:update', 'article:delete',
        'ai:use', 'media:upload', 'comment:audit',
        'category:manage', 'tag:manage', 'user:manage', 'stats:view'
      ]
      return perms.some((p) => creatorPerms.includes(p))
    }
  },

  actions: {
    /** 是否拥有指定权限点（任一命中即通过） */
    hasPermission(perm: string | string[]): boolean {
      const perms = this.userInfo?.permissions ?? []
      const required = Array.isArray(perm) ? perm : [perm]
      return required.some((p) => perms.includes(p))
    },
    /** 登录：调用 /api/auth/login，保存双令牌 */
    async login(params: LoginParams): Promise<void> {
      const res = await loginApi(params)
      setTokens({
        accessToken: res.accessToken,
        refreshToken: res.refreshToken,
        tokenType: res.tokenType,
        expiresIn: res.expiresIn
      })
      this.token = res.accessToken
    },

    /** 退出：注销后端 RefreshToken + 清本地 */
    async logout(): Promise<void> {
      try {
        await logoutApi()
      } finally {
        clearTokens()
        this.token = null
        this.userInfo = null
      }
    },

    /** 拉取当前用户信息（守卫进入时调用，失败由请求拦截器统一处理） */
    async fetchUserInfo(): Promise<void> {
      const info = await userInfoApi()
      this.userInfo = info
    },

    /** 设置用户信息（骨架阶段由路由守卫调用 /api/user/info） */
    setUserInfo(info: UserInfo) {
      this.userInfo = info
    }
  }
})
