<template>
  <el-aside :width="sidebarCollapsed ? '64px' : '240px'" class="sidebar">
    <!-- Logo 区 -->
    <div class="sidebar-logo" @click="router.push('/dashboard')">
      <span v-if="sidebarCollapsed" class="logo-mark">编</span>
      <template v-else>
        <span class="logo-mark">编</span>
        <span class="logo-text">CMS 编辑部</span>
      </template>
    </div>

    <!-- 菜单：从路由 meta 渲染 -->
    <el-menu
      class="sidebar-menu"
      :default-active="activePath"
      :collapse="sidebarCollapsed"
      :collapse-transition="false"
      router
    >
      <el-menu-item v-for="item in menus" :key="item.path" :index="'/' + item.path">
        <el-icon><component :is="item.meta?.icon || 'Document'" /></el-icon>
        <template #title>{{ item.meta?.title }}</template>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)

/** 菜单项：管理后台 Layout 的直接子路由，剔除隐藏项，并按权限点过滤（meta.permissions 未定义=所有登录用户可见） */
const menus = computed(() => {
  const root = router.options.routes.find((r) => r.path === '/')
  if (!root || !root.children) return []
  return root.children.filter((r) => {
    if (r.meta?.hidden || !r.meta?.title) return false
    const required = r.meta?.permissions as string[] | undefined
    return !required || required.some((p) => userStore.permissions.includes(p))
  })
})

const activePath = computed(() => route.path)
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.sidebar {
  background-color: $bg-page;
  border-right: 1px solid $border-default;
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: $header-height;
  padding: 0 16px;
  cursor: pointer;
  border-bottom: 1px solid $border-default;
  white-space: nowrap;
  overflow: hidden;
}

.logo-mark {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background-color: $brand-primary;
  color: #fff;
  font-family: $font-display;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.sidebar-menu {
  flex: 1;
  background-color: transparent;
  padding: 8px;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  border-radius: 4px;
  margin-bottom: 2px;
  font-size: 14px;
}
</style>
