<template>
  <div class="page-container placeholder-page">
    <!-- 空态 = 行动邀请（设计系统 §5） -->
    <div class="empty-card">
      <div class="empty-art">
        <span class="empty-mark">编</span>
      </div>
      <h2 class="empty-title">{{ route.meta.title || '功能开发中' }}</h2>
      <p class="empty-desc">本页面已纳入开发计划，骨架阶段先占位。后续按 v2.1 需求文档实现。</p>
      <el-button type="primary" @click="router.push('/dashboard')">返回仪表盘</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.placeholder-page {
  display: flex;
  min-height: calc(100vh - 96px);
}

.empty-card {
  margin: auto;
  text-align: center;
  max-width: 420px;
  padding: 40px 48px;
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-lg;
  box-sizing: border-box;
}

.empty-art {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  border-radius: $radius-md;
  background-color: $brand-light;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-mark {
  font-family: $font-display;
  font-size: 28px;
  color: $brand-primary;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 13px;
  color: $text-secondary;
  margin: 0 0 24px;
}
</style>
