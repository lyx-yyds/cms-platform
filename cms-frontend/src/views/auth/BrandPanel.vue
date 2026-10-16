<template>
  <!-- 认证页品牌区（编辑部 Editorial：无渐变、暖纸白、签名元素） -->
  <div class="brand-panel">
    <div class="brand-logo">
      <span class="logo-mark">编</span>
      <span class="brand-name">CMS 内容管理系统</span>
    </div>

    <h1 class="slogan">
      内容管理，<br />像编辑一份刊物。
    </h1>
    <p class="slogan-sub">
      稿件台 · 审核流 · AI 辅助写作，一个安静、专业的内容工作台。
    </p>

    <div class="brand-samples">
      <div class="sample-row status-draft">
        <span class="sample-id">#0241</span><span>草稿 · 等待落笔</span>
      </div>
      <div class="sample-row status-reviewing">
        <span class="sample-id">#0242</span><span>审核中 · 2 条批注</span>
      </div>
      <div class="sample-row status-published">
        <span class="sample-id">#0243</span><span>已发布 · 阅读 1.2k</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 纯展示组件：认证类页面（登录 / 注册 / 找回 / 重置）共用品牌区
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.brand-panel {
  flex: 0 0 56%;
  padding: 48px 64px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  box-sizing: border-box;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 48px;
}

.logo-mark {
  width: 32px;
  height: 32px;
  border-radius: $radius-sm;
  background-color: $brand-primary;
  color: #fff;
  font-family: $font-display;
  font-size: 17px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.slogan {
  font-size: 40px;
  line-height: 1.3;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 16px;
}

.slogan-sub {
  font-size: 14px;
  color: $text-secondary;
  margin: 0 0 40px;
  max-width: 340px;
}

.brand-samples {
  max-width: 300px;
}

.sample-row {
  position: relative;
  padding: 8px 0 8px 16px;
  font-size: 13px;
  color: $text-secondary;
  display: flex;
  gap: 10px;
}

.sample-row::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: 1px;
  background: var(--status-color);
}

.sample-id {
  font-family: $font-mono;
  font-size: 12px;
  color: $text-tertiary;
}

@media (max-width: 900px) {
  .brand-panel {
    flex: none;
    padding: 32px 24px;
  }

  .brand-samples {
    display: none;
  }
}
</style>
