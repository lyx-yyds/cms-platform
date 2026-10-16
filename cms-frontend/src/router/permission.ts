/**
 * 路由守卫（对应文档 router/permission.ts）
 * 未登录访问受保护页跳登录页；已登录访问登录页跳仪表盘
 * 放行规则：meta.public 的公开页（登录/注册/找回/重置）不校验登录态
 */
import router from './index'
import { isLoggedIn } from '@/utils/auth'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

router.beforeEach(async (to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title as string} · CMS 内容管理系统` : 'CMS 内容管理系统'

  const loggedIn = isLoggedIn()

  // 公开页（meta.public）：已登录访问登录页才重定向仪表盘，其余直接放行
  if (to.meta.public) {
    if (loggedIn && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
    return
  }

  if (!loggedIn) {
    ElMessage.warning('请先登录')
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    return
  }

  // 已登录：确保用户信息已加载（右上角显示昵称；失败静默，拦截器会处理 401）
  const userStore = useUserStore()
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // 刷新失败等场景由请求拦截器统一跳登录
    }
  }

  // 权限校验：目标路由声明了 meta.permissions 且当前用户权限点与之无交集时拦截（文档 7.3）
  // 权限点随角色权限编辑实时生效（sys_role_permission → /api/user/info permissions）
  const requiredPerms = (to.meta.permissions as string[] | undefined) ?? []
  if (requiredPerms.length > 0 && !requiredPerms.some((p) => userStore.permissions.includes(p))) {
    ElMessage.warning('无权限访问该页面')
    // 兜底跳转用户可访问页面：创作权限用户回文章管理，普通用户去内容门户（文档 7.3）
    next(userStore.isCreator ? '/article/list' : '/site')
    return
  }

  next()
})
