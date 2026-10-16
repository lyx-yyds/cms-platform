<template>
  <div class="site-shell">
    <!-- 门户顶栏 -->
    <header class="site-header">
      <div class="site-header-inner">
        <router-link to="/site" class="site-brand">
          <span class="brand-mark">编</span>
          <span class="brand-text">CMS 编辑部</span>
          <span class="brand-tag">内容创作门户</span>
        </router-link>

        <nav class="site-nav">
          <router-link to="/site" class="nav-link" :class="{ active: isHome }">首页</router-link>
          <router-link v-if="userStore.isLoggedIn && isCreator" to="/dashboard" class="nav-link">管理后台</router-link>
          <router-link v-else-if="userStore.isLoggedIn" to="/profile" class="nav-link">个人信息</router-link>
        </nav>

        <div class="site-user">
          <template v-if="userStore.isLoggedIn">
            <span class="user-chip">{{ userStore.displayName }}</span>
            <button class="text-btn" @click="onLogout">退出</button>
          </template>
          <template v-else>
            <router-link to="/login" class="text-btn">登录</router-link>
            <router-link to="/register" class="ghost-btn">注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="site-main">
      <router-view />
    </main>

    <footer class="site-footer">
      <span>CMS 内容管理系统 · 编辑部 Editorial 门户</span>
      <span class="foot-sep">·</span>
      <span>Spring AI + Vue3 + MySQL + Redis + MinIO</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isHome = computed(() => route.path === '/site')

/** 创作角色（拥有任一内容创作/管理权限点）才有后台入口；普通用户走内容门户 */
const isCreator = computed(() => userStore.isCreator)

// 公开路由守卫不预载用户信息，这里登录态下补充加载（顶栏显示昵称）
onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // 令牌失效等由请求拦截器统一处理
    }
  }
})

async function onLogout() {
  await userStore.logout()
  router.push('/site')
}
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.site-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: $bg-page;
}

/* ===== 顶栏 ===== */
.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background-color: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid $border-default;
}

.site-header-inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.site-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background-color: $brand-primary;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.brand-text {
  font-size: 17px;
  font-weight: 700;
  color: $text-primary;
  letter-spacing: 0.5px;
}

.brand-tag {
  font-size: 11px;
  color: $text-tertiary;
  border: 1px solid $border-strong;
  border-radius: 999px;
  padding: 2px 8px;
}

.site-nav {
  display: flex;
  gap: 6px;
  flex: 1;
}

.nav-link {
  font-size: 13.5px;
  color: $text-secondary;
  text-decoration: none;
  padding: 6px 10px;
  border-radius: $radius-sm;
  transition: color 0.15s, background-color 0.15s;

  &:hover {
    color: $brand-primary;
    background-color: $brand-light;
  }

  &.active {
    color: $brand-primary;
    font-weight: 600;
  }
}

.site-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-chip {
  font-size: 13px;
  color: $text-primary;
  background-color: $bg-subtle;
  border: 1px solid $border-default;
  border-radius: 999px;
  padding: 4px 12px;
}

.text-btn {
  font-size: 13px;
  color: $text-secondary;
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: none;
  padding: 6px 4px;

  &:hover {
    color: $brand-primary;
  }
}

.ghost-btn {
  font-size: 13px;
  color: $brand-primary;
  border: 1px solid $brand-primary;
  border-radius: $radius-sm;
  padding: 5px 14px;
  text-decoration: none;
  transition: background-color 0.15s, color 0.15s;

  &:hover {
    background-color: $brand-primary;
    color: #fff;
  }
}

/* ===== 主区 ===== */
.site-main {
  flex: 1;
  max-width: 1080px;
  width: 100%;
  margin: 0 auto;
  padding: 28px 20px 48px;
  box-sizing: border-box;
}

/* ===== 页脚 ===== */
.site-footer {
  border-top: 1px solid $border-default;
  padding: 18px 20px;
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  color: $text-tertiary;
  background-color: $bg-surface;
}

.foot-sep {
  color: $border-strong;
}
</style>
