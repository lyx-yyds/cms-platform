<template>
  <el-container class="admin-layout">
    <Sidebar />
    <el-container class="admin-main">
      <Header />
      <el-main class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
      <Footer />
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import Footer from './Footer.vue'
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.admin-layout {
  height: 100%;
}

.admin-main {
  flex-direction: column;
}

.admin-content {
  background-color: $bg-page;
  padding: 0;
  overflow-y: auto;
}

/* 页面切换动效（§8 唯一编排时刻之一：fade 200ms） */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
