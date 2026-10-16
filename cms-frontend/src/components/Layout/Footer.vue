<template>
  <el-footer class="admin-footer">
    CMS 内容管理系统 · 编辑部 Editorial
  </el-footer>
</template>

<script setup lang="ts">
// 底部
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

.admin-footer {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: $bg-page;
  border-top: 1px solid $border-default;
  color: $text-tertiary;
  font-size: 12px;
}
</style>
