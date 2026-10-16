/**
 * 路由入口（对应文档 router/index.ts）
 * 骨架阶段：登录页 + 管理后台 Layout + 仪表盘 / 文章管理（稿件台）+ 占位页
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/register.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/auth/forgot.vue'),
    meta: { title: '找回密码', public: true }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/auth/reset.vue'),
    meta: { title: '重置密码', public: true }
  },
  {
    path: '/site',
    name: 'Site',
    component: () => import('@/views/site/layout.vue'),
    meta: { title: '内容门户', public: true },
    children: [
      {
        path: '',
        name: 'SiteHome',
        component: () => import('@/views/site/index.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'article/:id',
        name: 'SiteArticle',
        component: () => import('@/views/site/article.vue'),
        meta: { title: '文章详情' }
      }
    ]
  },
  {
    path: '/',
    component: () => import('@/components/Layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'Odometer', permissions: ['stats:view'] }
      },
      {
        path: 'article/list',
        name: 'ArticleList',
        component: () => import('@/views/article/list.vue'),
        meta: { title: '文章管理', icon: 'Document', permissions: ['article:list'] }
      },
      {
        path: 'article/edit/:id?',
        name: 'ArticleEdit',
        component: () => import('@/views/article/edit.vue'),
        meta: { title: '文章编辑', icon: 'EditPen', hidden: true, permissions: ['article:create', 'article:update'] }
      },
      {
        path: 'category',
        name: 'Category',
        component: () => import('@/views/category/index.vue'),
        meta: { title: '分类管理', icon: 'Folder', permissions: ['category:manage'] }
      },
      {
        path: 'tag',
        name: 'Tag',
        component: () => import('@/views/tag/index.vue'),
        meta: { title: '标签管理', icon: 'PriceTag', permissions: ['tag:manage'] }
      },
      {
        path: 'comment',
        name: 'Comment',
        component: () => import('@/views/comment/index.vue'),
        meta: { title: '评论管理', icon: 'ChatDotRound', permissions: ['comment:audit'] }
      },
      {
        path: 'media',
        name: 'Media',
        component: () => import('@/views/media/index.vue'),
        meta: { title: '媒体库', icon: 'Picture', permissions: ['media:upload'] }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User', permissions: ['user:manage'] }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/role/index.vue'),
        meta: { title: '角色管理', icon: 'Avatar', permissions: ['user:manage'] }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人信息', hidden: true }
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/stats/index.vue'),
        meta: { title: '数据统计', icon: 'DataAnalysis', permissions: ['stats:view'] }
      },
      {
        path: 'ai/generations',
        name: 'AiGenerations',
        component: () => import('@/views/ai/generations.vue'),
        meta: { title: 'AI 中心', icon: 'MagicStick', permissions: ['ai:use'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

export default router
